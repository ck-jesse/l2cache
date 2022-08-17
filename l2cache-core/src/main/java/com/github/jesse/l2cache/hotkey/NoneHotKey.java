package com.github.jesse.l2cache.hotkey;

import com.github.jesse.l2cache.HotKey;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

/**
 * @Author: zengjucai
 * @Date: 2021/6/10 13:45
 * 京东热key探测
 */
@Slf4j
public class NoneHotKey implements HotKey {

    @Override
    public <K> boolean ifHotKey(K key, Function<K, Object> cacheKeyBuilder) {
        return false;
    }
}
