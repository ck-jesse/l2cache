package com.jd.platform.hotkey.client.cache;

/**
 * @author wuweifeng wrote on 2020-02-21
 * @version 1.0
 */
public interface LocalCache {

    Object get(String key);

    Object get(String key, Object defaultValue);

    void delete(String key);

    void set(String key, Object value);

    void set(String key, Object value, long expire);

    void removeAll();
}
