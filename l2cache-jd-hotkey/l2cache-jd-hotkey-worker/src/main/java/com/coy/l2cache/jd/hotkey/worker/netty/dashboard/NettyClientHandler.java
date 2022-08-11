package com.coy.l2cache.jd.hotkey.worker.netty.dashboard;

import com.coy.l2cache.jd.hotkey.common.model.HotKeyMsg;
import com.coy.l2cache.jd.hotkey.common.model.MsgBuilder;
import com.coy.l2cache.jd.hotkey.common.model.typeenum.MessageType;
import com.coy.l2cache.jd.hotkey.common.tool.Constant;
import com.coy.l2cache.jd.hotkey.common.tool.FastJsonUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wuweifeng wrote on 2019-11-05.
 */
@ChannelHandler.Sharable
public class NettyClientHandler extends SimpleChannelInboundHandler<String> {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;

            if (idleStateEvent.state() == IdleState.ALL_IDLE) {
                //向服务端发送消息
                ctx.writeAndFlush(MsgBuilder.buildByteBuf(new HotKeyMsg(MessageType.PING, Constant.PING)));
            }
        }

        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("channelActive:" + ctx.name());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        NettyClient.getInstance().disConnect();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String message) {
        HotKeyMsg msg = FastJsonUtils.toBean(message, HotKeyMsg.class);
        if (MessageType.PONG == msg.getMessageType()) {
            logger.info("heart beat");
        }

    }

}
