package com.coy.l2cache.jd.hotkey.client.etcd;

import com.coy.l2cache.jd.hotkey.common.configcenter.IConfigCenter;
import com.coy.l2cache.jd.hotkey.common.configcenter.etcd.JdEtcdBuilder;

/**
 * @author wuweifeng wrote on 2020-01-07
 * @version 1.0
 */
public class EtcdConfigFactory {
    private static IConfigCenter configCenter;

    private EtcdConfigFactory() {}

    public static IConfigCenter configCenter() {
        return configCenter;
    }

    public static void buildConfigCenter(String etcdServer) {
        //连接多个时，逗号分隔
        configCenter = JdEtcdBuilder.build(etcdServer);
    }
}
