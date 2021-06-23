package com.coy.l2cache;

import com.coy.l2cache.spi.SPI;
import com.coy.l2cache.sync.CacheMessage;
import com.coy.l2cache.sync.MessageListener;

import java.util.Map;
import java.util.function.Function;

/**
 * 热key自动识别
 *
 * @author zengjucai
 * @date 2021/6/10 13:45
 */
@SPI
public interface HotKey {

    /**
     * 是否为热key
     */
    <K> boolean ifHotKey(K key, Function<K, Object> cacheKeyBuilder);

}
