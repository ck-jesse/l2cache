package com.coy.l2cache.spring;

import cn.hutool.core.util.StrUtil;
import com.coy.l2cache.CacheConfig;
import com.jd.platform.hotkey.client.ClientStarter;
import com.jd.platform.hotkey.client.callback.JdHotKeyStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author wuweifeng wrote on 2020-01-14
 * @version 1.0
 */
@Configuration
@ConditionalOnProperty(name = "l2cache.config.jdHotKey.serviceName")
public class JdHotKeyConfiguration {

    @Autowired
    L2CacheProperties l2CacheProperties;

    @PostConstruct
    public void init() {
        CacheConfig cacheConfig = l2CacheProperties.getConfig();
        CacheConfig.JdHotKey jdHotKey = cacheConfig.getJdHotKey();
        if(StrUtil.isBlank(jdHotKey.getEtcdUrl())){
            throw new IllegalStateException("jdHotKey not found etcd url yaml config file:" + jdHotKey);
        }

        ClientStarter.Builder builder = new ClientStarter.Builder();
        ClientStarter starter = builder.setAppName(jdHotKey.getServiceName()).setEtcdServer(jdHotKey.getEtcdUrl()).build();
        starter.startPipeline();
    }
}
