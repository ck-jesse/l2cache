package com.coy.l2cache.load;

import lombok.Data;
import org.slf4j.MDC;

import java.util.Map;

/**
 * 对 缓存key 包装 MDC
 * <p>
 * 痛点：多线程环境下共用 CustomCacheLoader 对象，导致难以将mdc内容传递到FockJoinPool的线程中
 * 方案：此包装类的设计挺有意思，可借助该包装类解决Caffeine的CacheLoader模式通过FockJoinPool异步加载数据时的链路追踪问题。
 *
 * @author chenck
 * @date 2021/5/10 20:02
 */
@Data
public class KeyWarpper<K> {

    private K key;// 缓存key
    private Map<String, String> mdcContextMap;//

    public KeyWarpper(K key) {
        this.key = key;
        this.mdcContextMap = MDC.getCopyOfContextMap();
    }

}
