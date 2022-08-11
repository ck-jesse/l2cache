package com.jd.platform.hotkey.worker.starters;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.ibm.etcd.api.Event;
import com.ibm.etcd.api.KeyValue;
import com.ibm.etcd.client.kv.KvClient;
import com.ibm.etcd.client.kv.WatchUpdate;
import com.jd.platform.hotkey.common.configcenter.ConfigConstant;
import com.jd.platform.hotkey.common.configcenter.IConfigCenter;
import com.jd.platform.hotkey.common.rule.KeyRule;
import com.jd.platform.hotkey.common.tool.FastJsonUtils;
import com.jd.platform.hotkey.common.tool.IpUtils;
import com.jd.platform.hotkey.worker.cache.CaffeineCacheHolder;
import com.jd.platform.hotkey.worker.model.AppInfo;
import com.jd.platform.hotkey.worker.model.TotalCount;
import com.jd.platform.hotkey.worker.netty.dashboard.NettyClient;
import com.jd.platform.hotkey.worker.netty.filter.HotKeyFilter;
import com.jd.platform.hotkey.worker.netty.holder.ClientInfoHolder;
import com.jd.platform.hotkey.worker.netty.holder.WhiteListHolder;
import com.jd.platform.hotkey.worker.rule.KeyRuleHolder;
import com.jd.platform.hotkey.worker.tool.AsyncPool;
import com.jd.platform.hotkey.worker.tool.InitConstant;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.jd.platform.hotkey.worker.tool.InitConstant.*;

/**
 * worker端对etcd相关的处理
 *
 * @author wuweifeng wrote on 2019-12-10
 * @version 1.0
 */
@Component
public class EtcdStarter {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private IConfigCenter configCenter;

    @Value("${netty.port}")
    private int port;

    /**
     * 该worker放到etcd worker目录的哪个app下
     */
    @Value("${etcd.workerPath}")
    private String workerPath;

    @Value("${local.address}")
    private String localAddress;

    @Value("${open.monitor}")
    private boolean openMonitor;

    private static final String DEFAULT_PATH = "default";

    private static final String MAO = ":";
    private static final String ETCD_DOWN = "etcd is unConnected . please do something";
    private static final String EMPTY_RULE = "very important warn !!! rule info is null!!!";

    /**
     * 用来存储临时收到的key总量，来判断是否很久都没收到key了
     */
    private long tempTotalReceiveKeyCount;
    /**
     * 每次10秒没收到key发过来，就将这个加1，加到3时，就停止自己注册etcd 30秒
     */
    private int mayBeErrorTimes = 0;
    /**
     * 是否可以继续上报自己的ip
     */
    private volatile boolean canUpload = true;

    //Grant：分配一个租约。
    //Revoke：释放一个租约。
    //TimeToLive：获取剩余TTL时间。
    //Leases：列举所有etcd中的租约。
    //KeepAlive：自动定时的续约某个租约。
    //KeepAliveOnce：为某个租约续约一次。
    //Close：貌似是关闭当前客户端建立的所有租约。

    /**
     * 该worker是否只服务于一个应用
     */
    private boolean isForSingle() {
        return !DEFAULT_PATH.equals(workerPath);
    }

    /**
     * 启动回调监听器，监听rule变化
     */
    @PostConstruct
    public void watch() {
        AsyncPool.asyncDo(() -> {
            KvClient.WatchIterator watchIterator;
            if (isForSingle()) {
                watchIterator = configCenter.watch(ConfigConstant.rulePath + workerPath);
            } else {
                watchIterator = configCenter.watchPrefix(ConfigConstant.rulePath);
            }
            while (watchIterator.hasNext()) {
                WatchUpdate watchUpdate = watchIterator.next();
                List<Event> eventList = watchUpdate.getEvents();

                KeyValue keyValue = eventList.get(0).getKv();
                logger.info("rule changed : " + keyValue);

                try {
                    ruleChange(keyValue);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    /**
     * 启动回调监听器，监听白名单变化，只监听自己所在的app，白名单key不参与热key计算，直接忽略
     */
    @PostConstruct
    public void watchWhiteList() {
        AsyncPool.asyncDo(() -> {
            //获取所有白名单
            fetchWhite();

            KvClient.WatchIterator watchIterator = configCenter.watch(ConfigConstant.whiteListPath + workerPath);
            while (watchIterator.hasNext()) {
                WatchUpdate watchUpdate = watchIterator.next();
                logger.info("whiteList changed ");

                try {
                    fetchWhite();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void fetchWhite() {
        String value = configCenter.get(ConfigConstant.whiteListPath + workerPath);
        if (StrUtil.isNotEmpty(value)) {
            String[] list = value.split(",");
            for (String s : list) {
                WhiteListHolder.add(s);
            }
        }
    }

    /**
     * 每隔1分钟拉取一次，所有的app的rule
     */
    @Scheduled(fixedRate = 60000)
    public void pullRules() {
        try {
            if (isForSingle()) {
                String value = configCenter.get(ConfigConstant.rulePath + workerPath);
                if (!StrUtil.isEmpty(value)) {
                    List<KeyRule> keyRules = FastJsonUtils.toList(value, KeyRule.class);
                    KeyRuleHolder.put(workerPath, keyRules);
                }
            } else {
                List<KeyValue> keyValues = configCenter.getPrefix(ConfigConstant.rulePath);
                for (KeyValue keyValue : keyValues) {
                    ruleChange(keyValue);
                }
            }
        } catch (StatusRuntimeException ex) {
            logger.error(ETCD_DOWN);
        }

    }

    /**
     * 每隔10秒上传一下client的数量到etcd中
     */
    @Scheduled(fixedRate = 10000)
    public void uploadClientCount() {
        try {
            String ip = IpUtils.getIp();
            for (AppInfo appInfo : ClientInfoHolder.apps) {
                String appName = appInfo.getAppName();
                int count = appInfo.size();
                //即便是full gc也不能超过3秒
                configCenter.putAndGrant(ConfigConstant.clientCountPath + appName + "/" + ip, count + "", 13);
            }

            configCenter.putAndGrant(ConfigConstant.caffeineSizePath + ip, FastJsonUtils.convertObjectToJSON(CaffeineCacheHolder.getSize()), 13);

            //上报每秒QPS（接收key数量、处理key数量）
            String totalCount = FastJsonUtils.convertObjectToJSON(new TotalCount(HotKeyFilter.totalReceiveKeyCount.get(), totalDealCount.longValue()));
            configCenter.putAndGrant(ConfigConstant.totalReceiveKeyCount + ip, totalCount, 13);

            logger.info(totalCount + " expireCount:" + expireTotalCount + " offerCount:" + totalOfferCount);

            //如果是稳定一直有key发送的应用，建议开启该监控，以避免可能发生的网络故障
            if (openMonitor) {
                checkReceiveKeyCount();
            }
//            configCenter.putAndGrant(ConfigConstant.bufferPoolPath + ip, MemoryTool.getBufferPool() + "", 10);
        } catch (Exception ex) {
            logger.error(ETCD_DOWN);
        }
    }

    /**
     * 每隔30秒去获取一下dashboard的地址
     */
    @Scheduled(fixedRate = 30000)
    public void fetchDashboardIp() {
        try {
            //获取DashboardIp
            List<KeyValue> keyValues = configCenter.getPrefix(ConfigConstant.dashboardPath);

            //是空，给个警告
            if (CollectionUtil.isEmpty(keyValues)) {
                logger.warn("very important warn !!! Dashboard ip is null!!!");
                return;
            }

            String dashboardIp = keyValues.get(0).getValue().toStringUtf8();
            NettyClient.getInstance().connect(dashboardIp);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 校验一下receive的key数量，如果一段时间没变，考虑网络问题，就将worker注册自己到etcd的心跳给断掉30秒，让各client重连一下自己
     */
    private void checkReceiveKeyCount() {
        //如果一样，说明10秒没收到新key了
        if (tempTotalReceiveKeyCount == HotKeyFilter.totalReceiveKeyCount.get()) {
            if (canUpload) {
                mayBeErrorTimes++;
            }
        } else {
            tempTotalReceiveKeyCount = HotKeyFilter.totalReceiveKeyCount.get();
        }
        if (mayBeErrorTimes >= 6) {
            logger.error("network maybe error …… i stop the heartbeat to etcd");
            canUpload = false;
            new Thread(() -> {
                try {
                    Thread.sleep(35000);
                    canUpload = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
            //需要把注册ip到etcd停一段时间，让各client和自己断连，重新连接
            mayBeErrorTimes = 0;
            //清零各个数据
            tempTotalReceiveKeyCount = 0;
            HotKeyFilter.totalReceiveKeyCount.set(0);
            InitConstant.totalDealCount.reset();
            InitConstant.totalOfferCount.reset();
            InitConstant.expireTotalCount.reset();
        }


    }


    /**
     * rule发生变化时，更新缓存的rule
     */
    private synchronized void ruleChange(KeyValue keyValue) {
        String appName = keyValue.getKey().toStringUtf8().replace(ConfigConstant.rulePath, "");
        if (StrUtil.isEmpty(appName)) {
            return;
        }
        String ruleJson = keyValue.getValue().toStringUtf8();
        List<KeyRule> keyRules = FastJsonUtils.toList(ruleJson, KeyRule.class);
        KeyRuleHolder.put(appName, keyRules);
    }

    @PreDestroy
    public void removeNodeInfo() {
        try {
            String hostName = IpUtils.getHostName();
            configCenter.delete(ConfigConstant.workersPath + hostName);
            AsyncPool.shutDown();
        } catch (Exception e) {
            logger.error("worker connect to etcd failure");
        }
    }

    /**
     * 每隔一会去check一下，自己还在不在etcd里
     */
    @PostConstruct
    public void makeSureSelfOn() {
        //开启上传worker信息
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {

            try {
                if (canUpload) {
                    uploadSelfInfo();
                }
            } catch (Exception e) {
                //do nothing
            }

        }, 0, 5, TimeUnit.SECONDS);
    }

    /**
     * 通过http请求手工上传信息到etcd，适用于正常使用过程中，etcd挂掉，导致worker租期到期被删除，无法自动注册
     */
    private void uploadSelfInfo() {
        configCenter.putAndGrant(buildKey(), buildValue(), 8);
    }

    private String buildKey() {
        String hostName = IpUtils.getHostName();
        return ConfigConstant.workersPath + workerPath + "/" + hostName;
    }

    private String buildValue() {
        String ip;
        if (StrUtil.isNotEmpty(localAddress)) {
            ip = localAddress;
        } else {
            ip = IpUtils.getIp();
        }
        return ip + MAO + port;
    }

}