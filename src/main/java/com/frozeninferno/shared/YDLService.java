package com.frozeninferno.shared;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import java.util.List;

/**
 * Created by brian on 7/4/16.
 */
@RemoteServiceRelativePath("YDLService")
public interface YDLService extends RemoteService {
    String startDownload(String uuid);
    void setOptions(String uuid,List<String> s);
    void setURLs(String uuid,List<String> s);
    String sendId(String uuid);
    List<String> getFiles(String uuid);
    void unload(String uuid);
    boolean getSizeLimitReached(String uuid);
}
