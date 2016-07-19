package com.frozeninferno.shared;

import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.List;

public interface YDLServiceAsync {
    void setOptions(String uuid, List<String> s, AsyncCallback<Void> async);

    void setURLs(String uuid, List<String> s, AsyncCallback<Void> async);

    void startDownload(String uuid, AsyncCallback<String> async);

    void sendId(String uuid, AsyncCallback<String> async);

    void unload(String uuid, AsyncCallback<Void> async);

    void getFiles(String uuid, AsyncCallback<List<String>> async);

    void getSizeLimitReached(String uuid, AsyncCallback<Boolean> async);
}
