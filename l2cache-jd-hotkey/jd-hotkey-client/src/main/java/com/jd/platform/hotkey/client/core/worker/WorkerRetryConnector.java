package com.jd.platform.hotkey.client.core.worker;

import com.jd.platform.hotkey.client.log.JdLogger;
import com.jd.platform.hotkey.client.netty.NettyClient;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * worker重连器
 * @author wuweifeng
 * @version 1.0
 * @date 2020-04-28
 */
public class WorkerRetryConnector {

    /**
     * 定时去重连没连上的workers
     */
    public static void retryConnectWorkers() {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        //开启拉取etcd的worker信息，如果拉取失败，则定时继续拉取
        scheduledExecutorService.scheduleAtFixedRate(WorkerRetryConnector::reConnectWorkers, 30, 30, TimeUnit.SECONDS);
    }

    private static void reConnectWorkers() {
        List<String> nonList = WorkerInfoHolder.getNonConnectedWorkers();
        if (nonList.size() == 0) {
            return;
        }
        JdLogger.info(WorkerRetryConnector.class, "trying to reConnect to these workers :" + nonList);
        NettyClient.getInstance().connect(nonList);
    }
}
