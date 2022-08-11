package cn.weeget.hotkey.client.netty;

import cn.weeget.hotkey.client.Context;
import cn.weeget.hotkey.client.callback.ReceiveNewKeyEvent;
import cn.weeget.hotkey.client.core.eventbus.EventBusCenter;
import cn.weeget.hotkey.client.log.JdLogger;
import cn.weeget.hotkey.client.netty.event.ChannelInactiveEvent;
import cn.weeget.hotkey.common.model.HotKeyModel;
import cn.weeget.hotkey.common.model.HotKeyMsg;
import cn.weeget.hotkey.common.model.MsgBuilder;
import cn.weeget.hotkey.common.model.typeenum.MessageType;
import cn.weeget.hotkey.common.tool.Constant;
import cn.weeget.hotkey.common.tool.FastJsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * @author wuweifeng wrote on 2019-11-05.
 */
@ChannelHandler.Sharable
public class NettyClientHandler extends SimpleChannelInboundHandler<String> {

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
        JdLogger.info(getClass(), "channelActive:" + ctx.name());
        ctx.writeAndFlush(MsgBuilder.buildByteBuf(new HotKeyMsg(MessageType.APP_NAME, Context.APP_NAME)));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        //断线了，可能只是client和server断了，但都和etcd没断。也可能是client自己断网了，也可能是server断了
        //发布断线事件。后续10秒后进行重连，根据etcd里的worker信息来决定是否重连，如果etcd里没了，就不重连。如果etcd里有，就重连
        notifyWorkerChange(ctx.channel());
    }

    private void notifyWorkerChange(Channel channel) {
        EventBusCenter.getInstance().post(new ChannelInactiveEvent(channel));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String message) {
        HotKeyMsg msg = FastJsonUtils.toBean(message, HotKeyMsg.class);
        if (MessageType.PONG == msg.getMessageType()) {
            JdLogger.info(getClass(), "heart beat");
            return;
        }
        if (MessageType.RESPONSE_NEW_KEY == msg.getMessageType()) {
            JdLogger.info(getClass(), "receive new key : " + msg);
            HotKeyModel model = FastJsonUtils.toBean(msg.getBody(), HotKeyModel.class);
            EventBusCenter.getInstance().post(new ReceiveNewKeyEvent(model));
        }

    }

}
