package com.coy.l2cache.sync;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 缓存消息
 *
 * @author chenck
 * @date 2020/4/28 20:29
 */
@Getter
@Setter
@Accessors(chain = true)
public class CacheMessage implements Serializable {

    private static final long serialVersionUID = -1L;

    private String instanceId;// 缓存实例id
    private String cacheType;// 缓存类型
    private String cacheName;// 缓存名称
    private String optType;// 操作类型 refresh/clear
    private Object key;// 缓存key

    public CacheMessage() {

    }

    public CacheMessage(String instanceId, String cacheType, String cacheName, Object key, String optType) {
        this.instanceId = instanceId;
        this.cacheType = cacheType;
        this.cacheName = cacheName;
        this.key = key;
        this.optType = optType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", instanceId=").append(instanceId);
        sb.append(", cacheType=").append(cacheType);
        sb.append(", cacheName=").append(cacheName);
        sb.append(", optType=").append(optType);
        sb.append(", key=").append(key);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }

}
