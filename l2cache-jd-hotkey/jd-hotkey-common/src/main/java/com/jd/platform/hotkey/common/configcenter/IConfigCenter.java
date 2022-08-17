package com.jd.platform.hotkey.common.configcenter;

import com.ibm.etcd.api.KeyValue;
import com.ibm.etcd.api.Kv;
import com.ibm.etcd.client.kv.KvClient;

import java.util.List;

/**
 * @author wuweifeng wrote on 2019-12-09
 * @version 1.0
 */
public interface IConfigCenter {
    /**
     * 存入key，value
     */
    void put(String key, String value);

    /**
     * 存入key、value，和租约id
     */
    void put(String key, String value, long leaseId);

    /**
     * 删除一个lease
     */
    void revoke(long leaseId);

    /**
     * 存入key、value，和过期时间，单位是秒
     */
    long putAndGrant(String key, String value, long ttl);

    /**
     * 给key设置新的leaseId
     */
    long setLease(String key, long leaseId);

    void delete(String key);

    /**
     * 根据key，获取value
     */
    String get(String key);

    /**
     * 获取指定前缀的所有key-value
     */
    List<KeyValue> getPrefix(String key);

    /**
     * 监听key
     */
    KvClient.WatchIterator watch(String key);

    /**
     * 监听前缀为key的
     */
    KvClient.WatchIterator watchPrefix(String key);

    /**
     * 自动续约
     * @param frequencySecs 续约频率，最小是4秒，默认是5秒
     * @param minTtl 最小存活时间，最小是2秒，默认是10秒
     * @return 返回leaseId
     */
    long keepAlive(String key, String value, int frequencySecs, int minTtl) throws Exception;

    /**
     * 构建一个可自动续约的lease
     */
    long buildAliveLease(int frequencySecs, int minTtl) throws Exception;

    /**
     * 构建一个普通lease
     */
    long buildNormalLease(long ttl);

    /**
     * 判断剩余的过期时间
     */
    long timeToLive(long leaseId);

    /**
     * 根据key，获取value
     */
    KeyValue getKv(String key);

}
