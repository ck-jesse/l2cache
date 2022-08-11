package com.coy.l2cache.jd.hotkey.worker.starters;

import com.coy.l2cache.jd.hotkey.worker.netty.client.IClientChangeListener;
import com.coy.l2cache.jd.hotkey.worker.netty.filter.INettyMsgFilter;
import com.coy.l2cache.jd.hotkey.worker.netty.server.NodesServer;
import com.coy.l2cache.jd.hotkey.worker.tool.AsyncPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

/**
 * @author wuweifeng wrote on 2019-12-11
 * @version 1.0
 */
@Component
public class NodesServerStarter {
    @Value("${netty.port}")
    private int port;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private IClientChangeListener iClientChangeListener;
    @Resource
    private List<INettyMsgFilter> messageFilters;

    @PostConstruct
    public void start() {
        AsyncPool.asyncDo(() -> {
            logger.info("netty server is starting");

            NodesServer nodesServer = new NodesServer();
            nodesServer.setClientChangeListener(iClientChangeListener);
            nodesServer.setMessageFilters(messageFilters);
            try {
                nodesServer.startNettyServer(port);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
