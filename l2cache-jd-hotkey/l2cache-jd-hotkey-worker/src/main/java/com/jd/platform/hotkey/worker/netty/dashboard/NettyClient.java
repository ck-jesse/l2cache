package com.jd.platform.hotkey.worker.netty.dashboard;

import com.jd.platform.hotkey.common.tool.Constant;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * netty连接器
 *
 * @author wuweifeng wrote on 2019-11-05.
 */
public class NettyClient {
    private static final NettyClient nettyClient = new NettyClient();

    private Bootstrap bootstrap;


    public static NettyClient getInstance() {
        return nettyClient;
    }

    private NettyClient() {
        if (bootstrap == null) {
            bootstrap = initBootstrap();
        }
    }

    private Bootstrap initBootstrap() {
        //少线程
        EventLoopGroup group = new NioEventLoopGroup(2);

        Bootstrap bootstrap = new Bootstrap();
        NettyClientHandler nettyClientHandler = new NettyClientHandler();
        bootstrap.group(group).channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ByteBuf delimiter = Unpooled.copiedBuffer(Constant.DELIMITER.getBytes());
                        ch.pipeline()
                                .addLast(new DelimiterBasedFrameDecoder(Constant.MAX_LENGTH, delimiter))
                                .addLast(new StringDecoder())
                                //10秒没消息时，就发心跳包过去
                                .addLast(new IdleStateHandler(0, 0, 30))
                                .addLast(nettyClientHandler);
                    }
                });
        return bootstrap;
    }

    public synchronized void connect(String address) {
        if (DashboardHolder.hasConnected) {
            return;
        }
        String[] ss = address.split(":");
        try {
            ChannelFuture channelFuture = bootstrap.connect(ss[0], Integer.parseInt(ss[1])).sync();
            DashboardHolder.channel = channelFuture.channel();
            DashboardHolder.hasConnected = true;
        } catch (Exception e) {
            DashboardHolder.hasConnected = false;
            DashboardHolder.channel = null;
            e.printStackTrace();
        }

        //这一步就阻塞了
//            channelFuture.channel().closeFuture().sync();
        //当server断开后才会走下面的
//            System.out.println("server is down");
    }

    public synchronized void disConnect() {
        DashboardHolder.channel = null;
        DashboardHolder.hasConnected = false;
    }

}
