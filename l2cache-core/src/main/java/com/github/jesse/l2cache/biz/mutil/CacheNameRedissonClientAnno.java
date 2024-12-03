package com.github.jesse.l2cache.biz.mutil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于配置 cacheName 对应的 RedissonClient 实例id，用于处理一个服务里面多个redis实例的场景
 *
 * @author chenck
 * @date 2024/12/3 16:40
 */
@Target({ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface CacheNameRedissonClientAnno {

    /**
     * cacheName
     */
    String cacheName() default "";

    /**
     * cacheName 对应的 RedissonClient 实例id
     */
    String instanceId();
}
