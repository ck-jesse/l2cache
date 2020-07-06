package com.coy.l2cache.sync;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author chenck
 * @date 2020/4/28 20:29
 */
@Getter
@Setter
@Accessors(chain = true)
public class CacheMessage implements Serializable {

    private static final long serialVersionUID = 2103077364243459916L;

    private String instanceId;// 缓存实例id
    private String cacheType;// 缓存类型
    private String cacheName;// 缓存名称
    private Object key;// 缓存key
    private String optType;// 操作类型 refresh/clear

    public CacheMessage() {

    }

    public CacheMessage(String instanceId, String cacheType, String cacheName, Object key, String optType) {
        this.instanceId = instanceId;
        this.cacheType = cacheType;
        this.cacheName = cacheName;
        this.key = key;
        this.optType = optType;
    }

}
