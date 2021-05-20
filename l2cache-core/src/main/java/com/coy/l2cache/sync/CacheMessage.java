package com.coy.l2cache.sync;

import com.coy.l2cache.consts.CacheConsts;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.slf4j.MDC;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Map;

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
    private Map<String, String> mdcContextMap;

    public CacheMessage() {
        this.mdcContextMap = MDC.getCopyOfContextMap();
    }

    public CacheMessage(String instanceId, String cacheType, String cacheName, Object key, String optType) {
        this.instanceId = instanceId;
        this.cacheType = cacheType;
        this.cacheName = cacheName;
        this.key = key;
        this.optType = optType;
        this.mdcContextMap = MDC.getCopyOfContextMap();
    }

    public Map<String, String> getMdcContextMap() {
        if (CollectionUtils.isEmpty(mdcContextMap)) {
            return mdcContextMap;
        }
        // 便于区分操作本身日志和消息通知所触发的日志
        String sid = mdcContextMap.get(CacheConsts.SID);
        if (!StringUtils.isEmpty(sid)) {
            mdcContextMap.put(CacheConsts.SID, this.buildNewTraceId(sid));
        }
        String trace_id = mdcContextMap.get(CacheConsts.TRACE_ID);
        if (!StringUtils.isEmpty(trace_id)) {
            mdcContextMap.put(CacheConsts.TRACE_ID, this.buildNewTraceId(trace_id));
        }
        return mdcContextMap;
    }

    private String buildNewTraceId(String trace_id) {
        StringBuilder sb = new StringBuilder(CacheConsts.PREFIX);
        sb.append(CacheConsts.SPLIT);
        sb.append(trace_id);
        sb.append(CacheConsts.SPLIT);
        sb.append(this.getInstanceId());
        return sb.toString();
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
        sb.append(", mdcContextMap=").append(mdcContextMap);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }

}
