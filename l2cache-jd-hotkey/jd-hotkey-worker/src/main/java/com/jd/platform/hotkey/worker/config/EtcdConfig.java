package com.jd.platform.hotkey.worker.config;

import com.jd.platform.hotkey.common.configcenter.IConfigCenter;
import com.jd.platform.hotkey.common.configcenter.etcd.JdEtcdBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author wuweifeng wrote on 2019-12-06
 * @version 1.0
 */
@Configuration
public class EtcdConfig {
    @Value("${etcd.server}")
    private String etcdServer;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Bean
    public IConfigCenter client() {
        logger.info("etcd address : " + etcdServer);
        //连接多个时，逗号分隔
        return JdEtcdBuilder.build(etcdServer);
    }

}
