package com.jd.platform.hotkey.dashboard.etcd;

import com.jd.platform.hotkey.common.configcenter.ConfigConstant;
import com.jd.platform.hotkey.common.configcenter.IConfigCenter;
import com.jd.platform.hotkey.common.tool.IpUtils;
import com.jd.platform.hotkey.dashboard.netty.NodesServer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * dashboard也注册到etcd，供各个worker连接
 *
 * @author wuweifeng
 * @version 1.0
 * @date 2020-08-28
 */
@Component
public class EtcdRegister {
    @Resource
    private IConfigCenter configCenter;
    @Resource
    private NodesServer nodesServer;

    /**
     * 每隔一会去check一下，自己还在不在etcd里
     */
    @PostConstruct
    public void makeSureSelfOn() {
        new Thread(() -> nodesServer.startNettyServer(ConfigConstant.dashboardPort)).start();

        //开启上传worker信息
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {

            try {
                uploadSelfInfo();
            } catch (Exception e) {
                //do nothing
            }

        }, 3, 30, TimeUnit.SECONDS);
    }

    private void uploadSelfInfo() {
        configCenter.putAndGrant(buildKey(), buildValue(), 32);
    }

    private String buildKey() {
        String hostName = IpUtils.getHostName();
        return ConfigConstant.dashboardPath + hostName;
    }

    private String buildValue() {
        String ip = IpUtils.getIp();
        return ip + ":" + ConfigConstant.dashboardPort;
    }

}
