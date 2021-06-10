package com.coy.l2cache.hotkey;

import cn.hutool.core.util.ObjectUtil;
import com.coy.l2cache.HotKeyService;
import com.jd.platform.hotkey.client.callback.JdHotKeyStore;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

/**
 * @Author: zengjucai
 * @Date: 2021/6/10 13:45
 * 京东热key探测
 */
@Slf4j
public class NoneHotKeyService implements HotKeyService {

    @Override
    public <K> boolean ifHotKey(K key, Function<K, Object> cacheKeyBuilder) {
        log.warn("exec noneHotKeyService");
        return false;
    }
}
