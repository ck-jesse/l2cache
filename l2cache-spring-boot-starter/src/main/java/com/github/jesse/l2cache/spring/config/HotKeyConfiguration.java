package com.github.jesse.l2cache.spring.config;

import cn.hutool.core.util.ObjectUtil;
import com.github.jesse.l2cache.L2CacheConfig;
import com.github.jesse.l2cache.CacheSyncPolicy;
import com.github.jesse.l2cache.HotkeyService;
import com.github.jesse.l2cache.biz.CacheService;
import com.github.jesse.l2cache.spi.ServiceLoader;
import com.github.jesse.l2cache.spring.L2CacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author
 * @version 1.0
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "l2cache.config.hotkey.type")
public class HotKeyConfiguration {

    @Autowired
    L2CacheProperties l2CacheProperties;

    @Autowired
    ApplicationContext context;

    @PostConstruct
    public void init() {
        L2CacheConfig.Hotkey hotkey = l2CacheProperties.getConfig().getHotkey();
        if (ObjectUtil.isEmpty(hotkey.getType())) {
            log.error("未配置 hotkey type，不进行初始化");
            return;
        }

        HotkeyService hotkeyService = ServiceLoader.load(HotkeyService.class, hotkey.getType());
        if (ObjectUtil.isNull(hotkeyService)) {
            log.error("非法的 hotkey type,无匹配的HotkeyService实现类, hotkey type={}", hotkey.getType());
            return;
        }

        hotkeyService.init(hotkey, getAllCacheName());
        hotkeyService.setInstanceId(L2CacheConfig.INSTANCE_ID);
        hotkeyService.setCacheSyncPolicy(ServiceLoader.load(CacheSyncPolicy.class, l2CacheProperties.getConfig().getCacheSyncPolicy().getType()));

        log.info("Hotkey实例初始化成功, hotkey type={}", hotkey.getType());
    }

    /**
     * 从 CacheService 中获取 cacheName
     */
    public List<String> getAllCacheName() {
        // 从 CacheService 中获取 cacheName，而不是从yaml文件中获取，因为yaml文件中可能没有配置所有的cacheName
        Map<String, CacheService> cacheServiceMap = context.getBeansOfType(CacheService.class);

        List<String> cacheNameList = new ArrayList<>();
        String cacheName = null;
        for (Map.Entry<String, CacheService> entry : cacheServiceMap.entrySet()) {
            cacheName = entry.getValue().getCacheName();
            // 做一次检查，检查是否有设置重复的cacheName
            if (cacheNameList.contains(cacheName)) {
                log.warn("配置错误：配置了相同的cacheName={}", cacheName);
                continue;
            }
            cacheNameList.add(cacheName);
        }
        return cacheNameList;
    }
}
