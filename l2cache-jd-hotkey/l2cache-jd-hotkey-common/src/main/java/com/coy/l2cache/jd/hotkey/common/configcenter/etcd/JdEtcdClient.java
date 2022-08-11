package com.coy.l2cache.jd.hotkey.common.configcenter.etcd;

import cn.hutool.core.collection.CollectionUtil;
import com.coy.l2cache.jd.hotkey.common.configcenter.IConfigCenter;
import com.google.protobuf.ByteString;
import com.ibm.etcd.api.KeyValue;
import com.ibm.etcd.api.LeaseGrantResponse;
import com.ibm.etcd.api.RangeResponse;
import com.ibm.etcd.client.KvStoreClient;
import com.ibm.etcd.client.kv.KvClient;
import com.ibm.etcd.client.lease.LeaseClient;
import com.ibm.etcd.client.lease.PersistentLease;
import com.ibm.etcd.client.lock.LockClient;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * etcd客户端
 *
 * @author wuweifeng wrote on 2019-12-06
 * @version 1.0
 */
public class JdEtcdClient implements IConfigCenter {

    private KvClient kvClient;
    private LeaseClient leaseClient;
    private LockClient lockClient;


    public JdEtcdClient(KvStoreClient kvStoreClient) {
        this.kvClient = kvStoreClient.getKvClient();
        this.leaseClient = kvStoreClient.getLeaseClient();
        this.lockClient = kvStoreClient.getLockClient();
    }


    public LeaseClient getLeaseClient() {
        return leaseClient;
    }

    public void setLeaseClient(LeaseClient leaseClient) {
        this.leaseClient = leaseClient;
    }

    public KvClient getKvClient() {
        return kvClient;
    }

    public void setKvClient(KvClient kvClient) {
        this.kvClient = kvClient;
    }

    public LockClient getLockClient() {
        return lockClient;
    }

    public void setLockClient(LockClient lockClient) {
        this.lockClient = lockClient;
    }

    @Override
    public void put(String key, String value) {
        kvClient.put(ByteString.copyFromUtf8(key), ByteString.copyFromUtf8(value)).sync();
    }

    @Override
    public void put(String key, String value, long leaseId) {
        kvClient.put(ByteString.copyFromUtf8(key), ByteString.copyFromUtf8(value), leaseId).sync();
    }

    @Override
    public void revoke(long leaseId) {
        leaseClient.revoke(leaseId);
    }

    @Override
    public long putAndGrant(String key, String value, long ttl) {
        LeaseGrantResponse lease = leaseClient.grant(ttl).sync();
        put(key, value, lease.getID());
        return lease.getID();
    }

    @Override
    public long setLease(String key, long leaseId) {
        kvClient.setLease(ByteString.copyFromUtf8(key), leaseId);
        return leaseId;
    }

    @Override
    public void delete(String key) {
        kvClient.delete(ByteString.copyFromUtf8(key)).sync();
    }

    @Override
    public String get(String key) {
        RangeResponse rangeResponse = kvClient.get(ByteString.copyFromUtf8(key)).sync();
        List<KeyValue> keyValues = rangeResponse.getKvsList();

        if (CollectionUtil.isEmpty(keyValues)) {
            return null;
        }
        return keyValues.get(0).getValue().toStringUtf8();
    }

    @Override
    public KeyValue getKv(String key) {
        RangeResponse rangeResponse = kvClient.get(ByteString.copyFromUtf8(key)).sync();
        List<KeyValue> keyValues = rangeResponse.getKvsList();
        if (CollectionUtil.isEmpty(keyValues)) {
            return null;
        }
        return keyValues.get(0);
    }

    @Override
    public List<KeyValue> getPrefix(String key) {
        RangeResponse rangeResponse = kvClient.get(ByteString.copyFromUtf8(key)).asPrefix().sync();
        return rangeResponse.getKvsList();
    }

    @Override
    public KvClient.WatchIterator watch(String key) {
        return kvClient.watch(ByteString.copyFromUtf8(key)).start();
    }

    @Override
    public KvClient.WatchIterator watchPrefix(String key) {
        return kvClient.watch(ByteString.copyFromUtf8(key)).asPrefix().start();
    }

    @Override
    public long keepAlive(String key, String value, int frequencySecs, int minTtl) throws Exception {
        //minTtl秒租期，每frequencySecs秒续约一下
        PersistentLease lease = leaseClient.maintain().leaseId(System.currentTimeMillis()).keepAliveFreq(frequencySecs).minTtl(minTtl).start();
        long newId = lease.get(3L, SECONDS);
        put(key, value, newId);
        return newId;
    }

    @Override
    public long buildAliveLease(int frequencySecs, int minTtl) throws Exception {
        PersistentLease lease = leaseClient.maintain().leaseId(System.currentTimeMillis()).keepAliveFreq(frequencySecs).minTtl(minTtl).start();

        return lease.get(3L, SECONDS);
    }

    @Override
    public long buildNormalLease(long ttl) {
        LeaseGrantResponse lease = leaseClient.grant(ttl).sync();
        return lease.getID();
    }

    @Override
    public long timeToLive(long leaseId) {
        try {
            return leaseClient.ttl(leaseId).get().getTTL();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return 0L;
        }
    }
}
