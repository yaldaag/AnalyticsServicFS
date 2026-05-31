package com.example.analytics.cache;

public interface ResultPageCache {

    void put(String pageToken, CachedPage page);

    CachedPage get(String pageToken);
}
