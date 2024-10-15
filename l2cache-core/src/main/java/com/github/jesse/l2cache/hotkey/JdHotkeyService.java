package com.github.jesse.l2cache.hotkey;

import cn.hutool.core.util.StrUtil;
import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.HotkeyService;
import com.github.jesse.l2cache.consts.CacheConsts;
import com.jd.platform.hotkey.client.ClientStarter;
import com.jd.platform.hotkey.client.callback.JdHotKeyStore;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 京东热key探测
 * <p>
 * jd hotkey client 会将本地的hotkey统计信息，上报给到worker端，worker识别到是hotkey后，会通知到其他的client，将hotkey缓存到本地缓存
 * <p>
 * https://gitee.com/jd-platform-opensource/hotkey
 *
 * @Author: zengjucai
 * @Date: 2021/6/10 13:45
 */
@Slf4j
public class JdHotkeyService implements HotkeyService {

    @Override
    public void init(L2CacheConfig.Hotkey hotKeyConfig, List<String> cacheNameList) {
        L2CacheConfig.Hotkey.JdHotkey jdHotKey = hotKeyConfig.getJd();
        if (StrUtil.isBlank(jdHotKey.getEtcdUrl())) {
            throw new IllegalStateException("jdHotKey not found etcd url yaml config file:" + jdHotKey);
        }

        ClientStarter.Builder builder = new ClientStarter.Builder();
        ClientStarter starter = builder.setAppName(jdHotKey.getServiceName()).setEtcdServer(jdHotKey.getEtcdUrl()).build();
        starter.startPipeline();
    }

    @Override
    public boolean isHotkey(String level1CacheType, String cacheName, String key) {
        StringBuilder sb = new StringBuilder(cacheName).append(CacheConsts.SPLIT).append(key);
        return JdHotKeyStore.isHotKey(sb.toString());
    }
}
