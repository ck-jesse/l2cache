package com.jd.platform.hotkey.common.tool;

import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-04-27
 */
public class NettyIpUtil {
    /**
     * 从netty连接中读取ip地址
     */
    public static String clientIp(ChannelHandlerContext ctx) {
        try {
            InetSocketAddress insocket = (InetSocketAddress) ctx.channel()
                    .remoteAddress();
            return insocket.getAddress().getHostAddress();
        } catch (Exception e) {
            return "未知";
        }

    }
}
