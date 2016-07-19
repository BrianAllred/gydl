package com.frozeninferno.server.utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static org.apache.commons.io.FileUtils.sizeOfDirectory;
import static org.apache.commons.io.FileUtils.sizeOfDirectoryAsBigInteger;

/**
 * Static IO helper library
 */
public class FileHelper {

    /**
     * Recursively delete directory
     * @param path Directory to delete
     * @return True if directory and all sub-files/directories were deleted
     */
    public static boolean deleteDirectory(String path) {
        String fullPath = path;
        Path directory = Paths.get(fullPath);
        if (Files.exists(directory)) {
            try {
                Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    /**
     * Creates a path and all parent paths to that path
     * @param path Path to create
     * @return Whether path was created
     */
    public static String createDirectory(String path) {
        String fullPath = path;
        Path directory = Paths.get(fullPath);

        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                return e.getMessage();
            }
        }

        return "";
    }

    /**
     * Gets storage space used by a given path and all subfiles/paths
     * @param path Path to query
     * @return Amount of storage occupied by path
     */
    public static double getSpaceInGigs(Path path) {
        try {
            BigDecimal bigSize;
            long size = sizeOfDirectory(path.toFile());
            double gigSize;

            if (size < 0) {
                bigSize = new BigDecimal(sizeOfDirectoryAsBigInteger(path.toFile()));
                bigSize.divide(new BigDecimal(1073741824.0));
                gigSize = bigSize.doubleValue();
            } else {
                gigSize = size / 1073741824.0;
            }

            return gigSize;
        } catch (Exception e) {
            return 0;
        }
    }
}
