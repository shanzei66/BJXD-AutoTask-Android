package com.guyuexuan.bjxd.util;

public interface ApiCallback<T> {
    void onSuccess(T result);

    void onError(String error);
}