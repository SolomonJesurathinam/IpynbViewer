package com.solomonj.ipynbviewer;

public interface PostResponseCallback {

    void onResponse(String response);
    void onError(Exception e);


}
