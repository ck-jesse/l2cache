package com.jd.platform.hotkey.client.netty.event;

import io.netty.channel.Channel;

/**
 * 客户端netty断线事件
 * @author wuweifeng wrote on 2020-01-20
 * @version 1.0
 */
public class ChannelInactiveEvent {
    private Channel channel;

    public ChannelInactiveEvent(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
