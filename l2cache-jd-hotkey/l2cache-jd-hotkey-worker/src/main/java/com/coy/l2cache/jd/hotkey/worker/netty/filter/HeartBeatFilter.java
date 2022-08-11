package com.coy.l2cache.jd.hotkey.worker.netty.filter;

import com.coy.l2cache.jd.hotkey.common.model.HotKeyMsg;
import com.coy.l2cache.jd.hotkey.common.model.MsgBuilder;
import com.coy.l2cache.jd.hotkey.common.model.typeenum.MessageType;
import com.coy.l2cache.jd.hotkey.common.tool.FastJsonUtils;
import com.coy.l2cache.jd.hotkey.common.tool.flush.FlushUtil;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.coy.l2cache.jd.hotkey.common.tool.Constant.PONG;

/**
 * 心跳包处理
 * @author wuweifeng wrote on 2019-12-11
 * @version 1.0
 */
@Component
@Order(1)
public class HeartBeatFilter implements INettyMsgFilter {
    @Override
    public boolean chain(HotKeyMsg message, ChannelHandlerContext ctx) {
        if (MessageType.PING == message.getMessageType()) {
            String hotMsg = FastJsonUtils.convertObjectToJSON(new HotKeyMsg(MessageType.PONG, PONG));
            FlushUtil.flush(ctx, MsgBuilder.buildByteBuf(hotMsg));
            return false;
        }
        return true;

    }
}
