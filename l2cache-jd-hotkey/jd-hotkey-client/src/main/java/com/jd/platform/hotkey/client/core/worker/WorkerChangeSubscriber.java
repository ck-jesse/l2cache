package com.jd.platform.hotkey.client.core.worker;

import com.google.common.eventbus.Subscribe;
import com.jd.platform.hotkey.client.log.JdLogger;
import com.jd.platform.hotkey.client.netty.event.ChannelInactiveEvent;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * eventbus监听worker信息变动
 *
 * @author wuweifeng wrote on 2020-01-13
 * @version 1.0
 */
public class WorkerChangeSubscriber {

    /**
     * 监听worker信息变动
     */
    @Subscribe
    public void connectAll(WorkerInfoChangeEvent event) {
        List<String> addresses = event.getAddresses();
        if (addresses == null) {
            addresses = new ArrayList<>();
        }

        WorkerInfoHolder.mergeAndConnectNew(addresses);
    }

    /**
     * 当client与worker的连接断开后，删除
     */
    @Subscribe
    public void channelInactive(ChannelInactiveEvent inactiveEvent) {
        //获取断线的channel
        Channel channel = inactiveEvent.getChannel();
        InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress();
        String address = socketAddress.getHostName() + ":" + socketAddress.getPort();
        JdLogger.warn(getClass(), "this channel is inactive : " + socketAddress + " trying to remove this connection");

        WorkerInfoHolder.dealChannelInactive(address);
    }

}
