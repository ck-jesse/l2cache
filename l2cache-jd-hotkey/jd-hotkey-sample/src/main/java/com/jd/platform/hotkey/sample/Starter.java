package com.jd.platform.hotkey.sample;

import com.jd.platform.hotkey.client.ClientStarter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author wuweifeng wrote on 2020-01-14
 * @version 1.0
 */
@Component
public class Starter {
    @Value("${etcd.server}")
    private String etcd;
    @Value("${spring.application.name}")
    private String appName;

    @PostConstruct
    public void init() {
        ClientStarter.Builder builder = new ClientStarter.Builder();
        ClientStarter starter = builder.setAppName(appName).setEtcdServer(etcd).build();
        starter.startPipeline();
    }

    public static void main(String[] args) {
        Map<String, HashSet<String>> totalSkuSet = new HashMap<>();
        System.out.println(totalSkuSet.get("a"));
    }

}
