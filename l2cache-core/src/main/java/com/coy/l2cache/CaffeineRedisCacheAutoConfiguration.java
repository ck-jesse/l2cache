package com.coy.l2cache;

import com.coy.l2cache.context.CaffeineRedisCacheManager;
import com.coy.l2cache.context.ExtendCacheManager;
import com.coy.l2cache.listener.CacheMessageListener;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.stream.Collectors;

/**
 * @author chenck
 * @date 2020/4/29 10:58
 */
@EnableCaching // 启用spring-cache
@ComponentScan // 扫描
@Configuration
@ConditionalOnClass({Caffeine.class, CaffeineRedisCacheManager.class})
@ConditionalOnMissingBean(CacheManager.class)
@EnableConfigurationProperties(CaffeineRedisCacheProperties.class)
public class CaffeineRedisCacheAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CaffeineRedisCacheAutoConfiguration.class);

    @Autowired
    CaffeineRedisCacheProperties caffeineRedisCacheProperties;

    /**
     * 替换RedisTemplate的默认jdk序列化为json序列化
     */
    @Bean
    @ConditionalOnMissingBean(RedisTemplate.class)
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 使用Jackson2JsonRedisSerialize 替换默认序列化
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // 设置value的序列化规则和 key的序列化规则
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * 自定义缓存管理器
     */
    @Bean
    @ConditionalOnMissingBean(CacheManagerCustomizers.class)
    public CacheManagerCustomizers cacheManagerCustomizers(ObjectProvider<CacheManagerCustomizer<?>> customizers) {
        return new CacheManagerCustomizers(customizers.orderedStream().collect(Collectors.toList()));
    }

    /**
     * 移除监听器
     */
    @Bean
    public RemovalListener<Object, Object> removalListener() {
        return new RemovalListener<Object, Object>() {
            @Override
            public void onRemoval(@Nullable Object key, @Nullable Object value, @NonNull RemovalCause cause) {
                logger.debug("[RemovalListener] key={}, value={}", key, value);
            }
        };
    }

    /**
     * 定义 CacheManager
     */
    @Bean
    @ConditionalOnMissingBean
    public ExtendCacheManager cacheManager(RedisTemplate<Object, Object> redisTemplate,
                                           CacheManagerCustomizers customizers,
                                           RemovalListener<Object, Object> removalListener) {
        ExtendCacheManager cacheManager = new CaffeineRedisCacheManager(redisTemplate, caffeineRedisCacheProperties);

        if (null != removalListener) {
            cacheManager.setRemovalListener(removalListener);
        }

        // 扩展点，源码中有很多可以借鉴的点
        return customizers.customize(cacheManager);
    }

    /**
     * 定义redis topic监听器
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisTemplate<Object, Object> redisTemplate,
                                                                       ExtendCacheManager extendCacheManager) {
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory(redisTemplate.getConnectionFactory());
        CacheMessageListener cacheMessageListener = new CacheMessageListener(redisTemplate, extendCacheManager);
        redisMessageListenerContainer.addMessageListener(cacheMessageListener, new ChannelTopic(caffeineRedisCacheProperties.getRedis().getTopic()));
        return redisMessageListenerContainer;
    }

}
