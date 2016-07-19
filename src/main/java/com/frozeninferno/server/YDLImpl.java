package com.frozeninferno.server;

import com.frozeninferno.server.utils.FileHelper;
import com.frozeninferno.server.utils.WatchDir;
import com.frozeninferno.server.utils.WatchDirListener;
import com.frozeninferno.shared.YDLService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet implementing the YDLService
 */
public class YDLImpl extends RemoteServiceServlet implements YDLService, WatchDirListener {

    /**
     * Various fields mapped to a specific client instance
     */
    private Map<String, List<String>> optionsList = new HashMap<>();
    private Map<String, List<String>> URLList = new HashMap<>();
    private Map<String, List<String>> fileList = new HashMap<>();
    private Map<String, Double> size = new HashMap<>();
    private Map<String, Process> process = new HashMap<>();

    /**
     * The working path of the servlet
     */
    private String workingPath;

    /**
     * Overrides the default servlet get with download functionality
     * @param req The request header
     * @param resp The response header
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String fileName = req.getParameter("fileInfo1");
        System.out.format(req.getRequestURL().toString());
        System.out.format("Requested file " + fileName);
        File file = new File(fileName);
        System.out.format(file.getName());

        int BUFFER = 1024 * 100;
        resp.setContentType("application/octet-stream");
        resp.setHeader("Content-Disposition", "attachment; filename=" + "\"" + file.getName() + "\"");
        ServletOutputStream outputStream = resp.getOutputStream();
        resp.setContentLength(Long.valueOf(file.length()).intValue());
        resp.setBufferSize(BUFFER);

        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        BufferedOutputStream out = new BufferedOutputStream(outputStream);

        int count;
        byte[] buffer = new byte[BUFFER];
        while ((count = in.read(buffer)) > 0) {
            out.write(buffer, 0, count);
        }

        out.flush();
        out.close();
        outputStream.flush();
        outputStream.close();
        System.out.flush();
    }

    /**
     * Implementation of start download
     * Starts a download using youtube-dl and supplied parameters
     * @param uuid UUID of client instance requesting the download
     * @return The console output of the download
     */
    @Override
    public String startDownload(String uuid) {
        StringBuilder output = new StringBuilder();
        try {
            List<String> argsList = new ArrayList<>(optionsList.get(uuid));
            argsList.addAll(URLList.get(uuid));
            argsList.add(0, "/usr/bin/youtube-dl");
            ProcessBuilder processBuilder = new ProcessBuilder(argsList);
            processBuilder.directory(new File(workingPath));
            process.put(uuid, processBuilder.start());
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.get(uuid).getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
                output.append(System.getProperty("line.separator"));
            }
            return output.toString();
        } catch (IOException ex) {
            return ex.getMessage();
        }
    }

    /**
     * Set various options related to the download
     * @param uuid UUID of client instance
     * @param options List of options to pass to downloader
     */
    @Override
    public void setOptions(String uuid, List<String> options) {
        optionsList.put(uuid, options);
    }

    /**
     * Set the download URLs
     * @param uuid UUID of client instance
     * @param urls
     */
    @Override
    public void setURLs(String uuid, List<String> urls) {
        URLList.put(uuid, urls);
    }

    /**
     * Gets whether the size limit was reached for client instance
     * @param uuid UUID of client instance
     * @return True if size limit reached
     */
    @Override
    public boolean getSizeLimitReached(String uuid){
        double sizeLimit = Double.parseDouble(getServletConfig().getServletContext().getInitParameter("storageLimit"));
        return this.size.get(uuid) >= sizeLimit;
    }

    /**
     * Initial message from client instance
     * @param uuid UUID of client instance
     * @return Any error messages generated during initialization
     */
    @Override
    public String sendId(final String uuid) {
        this.size.put(uuid, 0.0);
        this.fileList.put(uuid, new ArrayList<String>());
        this.optionsList.put(uuid, new ArrayList<String>());
        this.URLList.put(uuid, new ArrayList<String>());
        this.workingPath = getServletConfig().getServletContext().getInitParameter("storagePath") + uuid;

        if (FileHelper.deleteDirectory(uuid)) {
            String ret = FileHelper.createDirectory(workingPath);
            try {
                Thread watchDirThread = new Thread(new WatchDir(uuid, Paths.get(workingPath), false, this));
                watchDirThread.start();
            } catch (IOException e) {
                ret = e.getMessage();
            }

            return ret;
        }

        return "";
    }

    /**
     * Get list of files downloaded for client
     * @param uuid UUID of client instance
     * @return List of all files downloaded for specific client instance
     */
    @Override
    public List<String> getFiles(String uuid) {
        return this.fileList.get(uuid);
    }

    /**
     * Fired when client instance disconnects
     * Cleans up resources
     * @param uuid UUID of client instance
     */
    @Override
    public void unload(String uuid) {
        if (process != null && process.get(uuid) != null) {
            process.get(uuid).destroy();
        }

        optionsList.remove(uuid);
        URLList.remove(uuid);
        fileList.remove(uuid);
        size.remove(uuid);
        process.remove(uuid);

        FileHelper.deleteDirectory(workingPath);
    }

    /**
     * Fired when a file entry is created
     * @param uuid UUID of client instance
     * @param child File entry created
     */
    @Override
    public void entryCreated(String uuid, Path child) {
        updateFileList(uuid, child);
    }

    /**
     * Fired when a file entry is deleted
     * @param uuid UUID of client instance
     * @param child File entry deleted
     */
    @Override
    public void entryDeleted(String uuid, Path child) {
        updateFileList(uuid, child);
    }

    /**
     * Fired when a file entry is modified
     * @param uuid UUID of client instance
     * @param child File entry modified
     */
    @Override
    public void entryModified(String uuid, Path child) {
        updateFileList(uuid, child);
    }

    /**
     * Updates the file list associated with client
     * @param uuid UUID of client instance
     * @param path Path of child updated
     */
    private void updateFileList(String uuid, Path path) {
        try {
            Path dir = path.getParent();
            this.fileList.put(uuid, new ArrayList<String>());
            for (File file : dir.toFile().listFiles()) {
                this.fileList.get(uuid).add(file.getAbsolutePath());
            }

            this.size.put(uuid, FileHelper.getSpaceInGigs(dir));
        } catch (Exception e) {
            this.fileList.put(uuid, new ArrayList<String>());
            this.size.put(uuid, 0.0);
        }
    }
}
