package com.github.jesse.l2cache.spring.biz;

import com.github.jesse.l2cache.biz.CacheService;
import com.github.jesse.l2cache.biz.mutil.CacheNameRedissonClientAnno;
import com.github.jesse.l2cache.biz.mutil.CacheNameRedissonClientInitService;
import com.github.jesse.l2cache.biz.mutil.CacheNameRedissonClientSupport;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import java.util.Map;

/**
 * 按照spring的方式，来加载多redis实例场景的初始化配置
 * <p>
 * 注：实现InitializingBean，方便在spring容器初始化完成后，执行初始化逻辑
 *
 * @author chenck
 * @date 2024/12/3 16:28
 */
@Slf4j
public class SpringCacheNameRedissonClientInitService implements CacheNameRedissonClientInitService, InitializingBean {

    ApplicationContext context;

    public SpringCacheNameRedissonClientInitService(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void initCacheNameRedissonClientMap() {
        Map<String, CacheService> cacheServiceMap = context.getBeansOfType(CacheService.class);

        CacheNameRedissonClientAnno anno = null;
        for (Map.Entry<String, CacheService> entry : cacheServiceMap.entrySet()) {
            // 获取注解
            anno = entry.getValue().getClass().getAnnotation(CacheNameRedissonClientAnno.class);
            if (null == anno) {
                continue;
            }

            // 注解上未配置 cacheName，则从CacheService中获取cacheName
            String cacheName = anno.cacheName();
            if (null == cacheName || cacheName.isEmpty()) {
                cacheName = entry.getValue().getCacheName();
            }
            log.info("[多redis实例场景] 解析cacheName与RedissonClient实例id配置, cacheName={}, instanceId={}", cacheName, anno.instanceId());
            CacheNameRedissonClientSupport.putCacheNameRedissonClient(cacheName, anno.instanceId());
        }
    }

    @Override
    public void initRedissonClientMap() {
        // 将所有的RedissonClient实例放入缓存，用于处理一个服务里面多个redisson实例的场景
        Map<String, RedissonClient> map = context.getBeansOfType(RedissonClient.class);
        map.forEach(CacheNameRedissonClientSupport::putRedissonClient);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.initCacheNameRedissonClientMap();
        this.initRedissonClientMap();
    }
}
