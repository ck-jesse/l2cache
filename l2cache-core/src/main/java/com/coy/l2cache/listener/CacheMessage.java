package com.coy.l2cache.listener;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author chenck
 * @date 2020/4/28 20:29
 */
@Getter
@Setter
public class CacheMessage implements Serializable {

    private static final long serialVersionUID = 2103077364243459916L;

    private String instanceId;// 缓存实例id
    private String cacheName;// 缓存名称
    private Object key;// 缓存key
    private String optType;// 操作类型 refresh/clear

    public CacheMessage() {

    }

    public CacheMessage(String instanceId, String cacheName, Object key, String optType) {
        super();
        this.instanceId = instanceId;
        this.cacheName = cacheName;
        this.key = key;
        this.optType = optType;
    }

}
