package com.jd.platform.hotkey.worker.model;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * @author wuweifeng wrote on 2019-12-05
 * @version 1.0
 */
public class AppInfo {
    /**
     * 应用名
     */
    private String appName;
    /**
     * 某app的全部channel
     */
    private ChannelGroup channelGroup;

    public AppInfo(String appName) {
        this.appName = appName;
        channelGroup  = new DefaultChannelGroup(appName, GlobalEventExecutor.INSTANCE);
    }

    public void groupPush(Object object) {
        channelGroup.writeAndFlush(object);
    }

    public void add(ChannelHandlerContext ctx) {
        channelGroup.add(ctx.channel());
    }

    public void remove(ChannelHandlerContext ctx) {
        channelGroup.remove(ctx.channel());
    }

    public String getAppName() {
        return appName;
    }

    public int size() {
        return channelGroup.size();
    }

}
