package com.jd.platform.hotkey.client;

import com.jd.platform.hotkey.client.callback.ReceiveNewKeySubscribe;
import com.jd.platform.hotkey.client.core.eventbus.EventBusCenter;
import com.jd.platform.hotkey.client.core.key.PushSchedulerStarter;
import com.jd.platform.hotkey.client.core.rule.KeyRuleHolder;
import com.jd.platform.hotkey.client.core.worker.WorkerChangeSubscriber;
import com.jd.platform.hotkey.client.core.worker.WorkerRetryConnector;
import com.jd.platform.hotkey.client.etcd.EtcdConfigFactory;
import com.jd.platform.hotkey.client.etcd.EtcdStarter;
import com.jd.platform.hotkey.client.log.JdLogger;

/**
 * 客户端启动器
 *
 * @author wuweifeng wrote on 2019-12-05
 * @version 1.0
 */
public class ClientStarter {

    private String etcdServer;

    /**
     * 推送key的间隔(毫秒)，推送越快，探测的越密集，会越快探测出来，但对client资源消耗相应增大
     */
    private Long pushPeriod;
    /**
     * caffeine的最大容量，默认给5万
     */
    private int caffeineSize;

    public ClientStarter(String appName) {
        if (appName == null) {
            throw new NullPointerException("APP_NAME cannot be null!");
        }
        Context.APP_NAME = appName;
    }

    public static class Builder {
        private String appName;
        private String etcdServer;
        private Long pushPeriod;
        private int caffeineSize = 200000;

        public Builder() {
        }

        public Builder setAppName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder setCaffeineSize(int caffeineSize) {
            if (caffeineSize < 128) {
                caffeineSize = 128;
            }
            this.caffeineSize = caffeineSize;
            return this;
        }

        public Builder setEtcdServer(String etcdServer) {
            this.etcdServer = etcdServer;
            return this;
        }

        public Builder setPushPeriod(Long pushPeriod) {
            this.pushPeriod = pushPeriod;
            return this;
        }

        public ClientStarter build() {
            ClientStarter clientStarter = new ClientStarter(appName);
            clientStarter.etcdServer = etcdServer;
            clientStarter.pushPeriod = pushPeriod;
            clientStarter.caffeineSize = caffeineSize;

            return clientStarter;
        }

    }

    /**
     * 启动监听etcd
     */
    public void startPipeline() {
        JdLogger.info(getClass(), "etcdServer:" + etcdServer);
        //设置caffeine的最大容量
        Context.CAFFEINE_SIZE = caffeineSize;

        //设置etcd地址
        EtcdConfigFactory.buildConfigCenter(etcdServer);
        //开始定时推送
        PushSchedulerStarter.startPusher(pushPeriod);
        PushSchedulerStarter.startCountPusher(10);
        //开启worker重连器
        WorkerRetryConnector.retryConnectWorkers();

        registEventBus();

        EtcdStarter starter = new EtcdStarter();
        //与etcd相关的监听都开启
        starter.start();
    }

    private void registEventBus() {
        //netty连接器会关注WorkerInfoChangeEvent事件
        EventBusCenter.register(new WorkerChangeSubscriber());
        //热key探测回调关注热key事件
        EventBusCenter.register(new ReceiveNewKeySubscribe());
        //Rule的变化的事件
        EventBusCenter.register(new KeyRuleHolder());
    }


}
