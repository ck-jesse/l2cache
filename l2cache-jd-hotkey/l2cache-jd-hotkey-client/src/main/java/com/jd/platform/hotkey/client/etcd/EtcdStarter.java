package com.jd.platform.hotkey.client.etcd;

import cn.hutool.core.collection.CollectionUtil;
import com.ibm.etcd.api.Event;
import com.ibm.etcd.api.KeyValue;
import com.ibm.etcd.client.kv.KvClient;
import com.ibm.etcd.client.kv.WatchUpdate;
import com.jd.platform.hotkey.client.Context;
import com.jd.platform.hotkey.client.callback.ReceiveNewKeyEvent;
import com.jd.platform.hotkey.client.core.eventbus.EventBusCenter;
import com.jd.platform.hotkey.client.core.rule.KeyRuleInfoChangeEvent;
import com.jd.platform.hotkey.client.core.worker.WorkerInfoChangeEvent;
import com.jd.platform.hotkey.client.core.worker.WorkerInfoHolder;
import com.jd.platform.hotkey.client.log.JdLogger;
import com.jd.platform.hotkey.common.configcenter.ConfigConstant;
import com.jd.platform.hotkey.common.configcenter.IConfigCenter;
import com.jd.platform.hotkey.common.model.HotKeyModel;
import com.jd.platform.hotkey.common.rule.KeyRule;
import com.jd.platform.hotkey.common.tool.Constant;
import com.jd.platform.hotkey.common.tool.FastJsonUtils;
import io.grpc.StatusRuntimeException;
import io.netty.util.internal.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * etcd连接管理器
 *
 * @author wuweifeng wrote on 2019-12-10
 * @version 1.0
 */
public class EtcdStarter {

    public void start() {
        fetchWorkerInfo();

        fetchRule();

//        startWatchWorker();

        startWatchRule();

        //监听热key事件，只监听手工添加、删除的key
        startWatchHotKey();
    }

    /**
     * 启动后先拉取已存在的热key
     */
    private void fetchExistHotKey() {
        JdLogger.info(getClass(), "--- begin fetch exist hotKey from etcd ----");
        IConfigCenter configCenter = EtcdConfigFactory.configCenter();
        try {
            //获取所有热key
            List<KeyValue> handKeyValues = configCenter.getPrefix(ConfigConstant.hotKeyPath + Context.APP_NAME);
            List<KeyValue> workerKeyValues = configCenter.getPrefix(ConfigConstant.hotKeyRecordPath + Context.APP_NAME);

            for (KeyValue keyValue : handKeyValues) {
                String key = keyValue.getKey().toStringUtf8().replace(ConfigConstant.hotKeyPath + Context.APP_NAME + "/", "");
                HotKeyModel model = new HotKeyModel();
                model.setRemove(false);
                model.setKey(key);
                EventBusCenter.getInstance().post(new ReceiveNewKeyEvent(model));
            }
            for (KeyValue keyValue : workerKeyValues) {
                String key = keyValue.getKey().toStringUtf8().replace(ConfigConstant.hotKeyRecordPath + Context.APP_NAME + "/", "");
                HotKeyModel model = new HotKeyModel();
                model.setRemove(false);
                model.setKey(key);
                EventBusCenter.getInstance().post(new ReceiveNewKeyEvent(model));
            }
        } catch (StatusRuntimeException ex) {
            //etcd连不上
            JdLogger.error(getClass(), "etcd connected fail. Check the etcd address!!!");
        }

    }

    /**
     * 异步开始监听worker变化信息
     */
//    private void startWatchWorker() {
//        CompletableFuture.runAsync(() -> {
//            JdLogger.info(getClass(), "--- begin watch worker change ----");
//            IConfigCenter configCenter = EtcdConfigFactory.configCenter();
//
//            try {
//                //注意监听是只监听自己appName的，不监听default目录。但是下面的定时任务是如果appName下没有worker，就用default的
//                //这样譬如appName没有自己的专属worker，也可以用默认的default。但一旦将来有了自己的worker，就可以立刻监听到，就不再用default了
//                KvClient.WatchIterator watchIterator = configCenter.watchPrefix(ConfigConstant.workersPath + Context.APP_NAME);
//                while (watchIterator.hasNext()) {
//                    synchronized (LOCK) {
//                        WatchUpdate watchUpdate = watchIterator.next();
//                        List<Event> eventList = watchUpdate.getEvents();
//                        Event event = eventList.get(0);
//                        KeyValue keyValue = event.getKv();
//                        //value里放的是ip地址
//                        String ipPort = keyValue.getValue().toStringUtf8();
//                        if (Event.EventType.PUT.equals(event.getType())) {
//                            JdLogger.info(getClass(), "worker created ：" + ipPort);
//                            List<KeyValue> keyValues = configCenter.getPrefix(ConfigConstant.workersPath + Context.APP_NAME);
//                            List<String> addresses = new ArrayList<>();
//                            for (KeyValue one : keyValues) {
//                                //value里放的是ip地址
//                                addresses.add(one.getValue().toStringUtf8());
//                            }
//                            notifyWorkerChange(addresses);
//                        } else if (Event.EventType.DELETE.equals(event.getType())) {
//                            JdLogger.info(getClass(), "worker removed ：" + ipPort);
//                            WorkerInfoHolder.dealChannelInactive(ipPort);
//                        }
//                    }
//
//                }
//
//            } catch (Exception e) {
//                JdLogger.error(getClass(), "watch err");
//            }
//        });
//
//    }


    /**
     * 每隔30秒拉取worker信息
     */
    private void fetchWorkerInfo() {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        //开启拉取etcd的worker信息，如果拉取失败，则定时继续拉取
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            JdLogger.info(getClass(), "trying to connect to etcd and fetch worker info");
            fetch();

        }, 0, 30, TimeUnit.SECONDS);
    }

    private void fetch() {
        IConfigCenter configCenter = EtcdConfigFactory.configCenter();

        try {
            //获取所有worker的ip
            List<KeyValue> keyValues = configCenter.getPrefix(ConfigConstant.workersPath + Context.APP_NAME);
            //worker为空，可能该APP没有自己的worker集群，就去连默认的，如果默认的也没有，就不管了，等着心跳
            if (CollectionUtil.isEmpty(keyValues)) {
                keyValues = configCenter.getPrefix(ConfigConstant.workersPath + "default");
            }
            //全是空，给个警告
            if (CollectionUtil.isEmpty(keyValues)) {
                JdLogger.warn(getClass(), "very important warn !!! workers ip info is null!!!");
            }

            List<String> addresses = new ArrayList<>();
            if (keyValues != null) {
                for (KeyValue keyValue : keyValues) {
                    //value里放的是ip地址
                    String ipPort = keyValue.getValue().toStringUtf8();
                    addresses.add(ipPort);
                }
            }

            JdLogger.info(getClass(), "worker info list is : " + addresses + ", now addresses is "
                    + WorkerInfoHolder.getWorkers());
            //发布workerinfo变更信息
            notifyWorkerChange(addresses);
        } catch (StatusRuntimeException ex) {
            //etcd连不上
            JdLogger.error(getClass(), "etcd connected fail. Check the etcd address!!!");
        }

    }

    private void notifyWorkerChange(List<String> addresses) {
        EventBusCenter.getInstance().post(new WorkerInfoChangeEvent(addresses));
    }

    private void notifyRuleChange(List<KeyRule> rules) {
        EventBusCenter.getInstance().post(new KeyRuleInfoChangeEvent(rules));
    }

    /**
     * 异步开始监听热key变化信息，该目录里只有手工添加的key信息
     */
    private void startWatchHotKey() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            JdLogger.info(getClass(), "--- begin watch hotKey change ----");
            IConfigCenter configCenter = EtcdConfigFactory.configCenter();
            try {
                KvClient.WatchIterator watchIterator = configCenter.watchPrefix(ConfigConstant.hotKeyPath + Context.APP_NAME);
                //如果有新事件，即新key产生或删除
                while (watchIterator.hasNext()) {
                    WatchUpdate watchUpdate = watchIterator.next();

                    List<Event> eventList = watchUpdate.getEvents();
                    KeyValue keyValue = eventList.get(0).getKv();
                    Event.EventType eventType = eventList.get(0).getType();
                    try {
                        String key = keyValue.getKey().toStringUtf8().replace(ConfigConstant.hotKeyPath + Context.APP_NAME + "/", "");

                        //如果是删除key，就立刻删除
                        if (Event.EventType.DELETE == eventType) {
                            HotKeyModel model = new HotKeyModel();
                            model.setRemove(true);
                            model.setKey(key);
                            EventBusCenter.getInstance().post(new ReceiveNewKeyEvent(model));
                        } else {
                            HotKeyModel model = new HotKeyModel();
                            model.setRemove(false);
                            String value = keyValue.getValue().toStringUtf8();
                            //新增热key
                            JdLogger.info(getClass(), "etcd receive new key : " + key + " --value:" + value);
                            //如果这是一个删除指令，就什么也不干
                            if (Constant.DEFAULT_DELETE_VALUE.equals(value)) {
                                continue;
                            }

                            //手工创建的value是时间戳
                            model.setCreateTime(Long.valueOf(keyValue.getValue().toStringUtf8()));

                            model.setKey(key);
                            EventBusCenter.getInstance().post(new ReceiveNewKeyEvent(model));
                        }
                    } catch (Exception e) {
                        JdLogger.error(getClass(), "new key err ：" + keyValue);
                    }

                }
            } catch (Exception e) {
                JdLogger.error(getClass(), "watch err");
            }
        });

    }

    private void fetchRule() {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        //开启拉取etcd的worker信息，如果拉取失败，则定时继续拉取
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            JdLogger.info(getClass(), "trying to connect to etcd and fetch rule info");
            boolean success = fetchRuleFromEtcd();
            if (success) {
                //拉取已存在的热key
                fetchExistHotKey();

                scheduledExecutorService.shutdown();
            }

        }, 0, 5, TimeUnit.SECONDS);
    }

    private boolean fetchRuleFromEtcd() {
        IConfigCenter configCenter = EtcdConfigFactory.configCenter();
        try {
            List<KeyRule> ruleList = new ArrayList<>();
            //从etcd获取自己的rule
            String rules = configCenter.get(ConfigConstant.rulePath + Context.APP_NAME);
            if (StringUtil.isNullOrEmpty(rules)) {
                JdLogger.warn(getClass(), "rule is empty");
                //会清空本地缓存队列
                notifyRuleChange(ruleList);
                return true;
            }
            ruleList = FastJsonUtils.toList(rules, KeyRule.class);

            notifyRuleChange(ruleList);
            return true;
        } catch (StatusRuntimeException ex) {
            //etcd连不上
            JdLogger.error(getClass(), "etcd connected fail. Check the etcd address!!!");
            return false;
        } catch (Exception e) {
            JdLogger.error(getClass(), "fetch rule failure, please check the rule info in etcd");
            return true;
        }

    }

    /**
     * 异步监听rule规则变化
     */
    private void startWatchRule() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            JdLogger.info(getClass(), "--- begin watch rule change ----");
            try {
                IConfigCenter configCenter = EtcdConfigFactory.configCenter();
                KvClient.WatchIterator watchIterator = configCenter.watch(ConfigConstant.rulePath + Context.APP_NAME);
                //如果有新事件，即rule的变更，就重新拉取所有的信息
                while (watchIterator.hasNext()) {
                    //这句必须写，next会让他卡住，除非真的有新rule变更
                    WatchUpdate watchUpdate = watchIterator.next();
                    List<Event> eventList = watchUpdate.getEvents();
                    JdLogger.info(getClass(), "rules info changed. begin to fetch new infos. rule change is " + eventList);

                    //全量拉取rule信息
                    fetchRuleFromEtcd();
                }
            } catch (Exception e) {
                JdLogger.error(getClass(), "watch err");
            }


        });
    }

}