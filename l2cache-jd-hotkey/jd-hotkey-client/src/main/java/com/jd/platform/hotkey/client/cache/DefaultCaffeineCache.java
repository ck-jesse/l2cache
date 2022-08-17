package com.jd.platform.hotkey.client.cache;

/**
 * @author wuweifeng wrote on 2020-02-27
 * @version 1.0
 */
public class DefaultCaffeineCache extends CaffeineCache {
    public DefaultCaffeineCache() {
        this(60);
    }

    public DefaultCaffeineCache(int duration) {
        super(duration);
    }
}
