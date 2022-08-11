package cn.weeget.hotkey.dashboard.netty;

import cn.weeget.hotkey.common.model.HotKeyModel;
import cn.weeget.hotkey.common.model.HotKeyMsg;
import cn.weeget.hotkey.common.model.MsgBuilder;
import cn.weeget.hotkey.common.model.typeenum.MessageType;
import cn.weeget.hotkey.common.tool.FastJsonUtils;
import cn.weeget.hotkey.common.tool.flush.FlushUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.List;

import static cn.weeget.hotkey.common.tool.Constant.PONG;

/**
 * 这里处理所有netty事件。
 *
 * @author wuweifeng wrote on 2019-11-05.
 */
public class NodesServerHandler extends SimpleChannelInboundHandler<String> {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String message) {
        if (StringUtils.isEmpty(message)) {
            return;
        }
        try {
            HotKeyMsg msg = FastJsonUtils.toBean(message, HotKeyMsg.class);
            if (MessageType.PING == msg.getMessageType()) {
                String hotMsg = FastJsonUtils.convertObjectToJSON(new HotKeyMsg(MessageType.PONG, PONG));
                FlushUtil.flush(ctx, MsgBuilder.buildByteBuf(hotMsg));
            } else if (MessageType.REQUEST_HOT_KEY == msg.getMessageType()) {
                List<HotKeyModel> list = FastJsonUtils.toList(msg.getBody(), HotKeyModel.class);
                for (HotKeyModel hotKeyModel : list) {
                    logger.info("dashboard receive model={}", FastJsonUtils.convertObjectToJSON(hotKeyModel));
                    HotKeyReceiver.push(hotKeyModel);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("some thing is error , " + cause.getMessage());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
        super.channelInactive(ctx);
    }

}
