package com.jd.platform.hotkey.common.tool.flush;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wuweifeng wrote on 2019-12-11
 * @version 1.0
 */
public class FlushUtil {
    private static Logger logger = LoggerFactory.getLogger("flushUtil");

    /**
     * 往channel里输出消息
     */
    public static void flush(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) {
//        if (channelHandlerContext.channel().isWritable()) {
//            channelHandlerContext.channel().writeAndFlush(byteBuf).addListener(future -> {
//                if (!future.isSuccess()) {
//                    logger.warn("flush error " + future.cause().getMessage());
//                }
//            });
//        } else {
            try {
                //同步发送
                channelHandlerContext.channel().writeAndFlush(byteBuf).sync();
            } catch (InterruptedException e) {
                logger.error("flush error " + e.getMessage());
            }
//        }
    }
}
