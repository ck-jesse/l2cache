package com.jd.platform.hotkey.worker.netty.filter;

import cn.hutool.core.date.SystemClock;
import com.jd.platform.hotkey.common.model.HotKeyModel;
import com.jd.platform.hotkey.common.model.HotKeyMsg;
import com.jd.platform.hotkey.common.model.typeenum.MessageType;
import com.jd.platform.hotkey.common.tool.FastJsonUtils;
import com.jd.platform.hotkey.common.tool.NettyIpUtil;
import com.jd.platform.hotkey.worker.keydispatcher.KeyProducer;
import com.jd.platform.hotkey.worker.netty.holder.WhiteListHolder;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 热key消息，包括从netty来的和mq来的。收到消息，都发到队列去
 *
 * @author wuweifeng wrote on 2019-12-11
 * @version 1.0
 */
@Component
@Order(3)
public class HotKeyFilter implements INettyMsgFilter {
    @Resource
    private KeyProducer keyProducer;

    public static AtomicLong totalReceiveKeyCount = new AtomicLong();

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public boolean chain(HotKeyMsg message, ChannelHandlerContext ctx) {
        if (MessageType.REQUEST_NEW_KEY == message.getMessageType()) {
            totalReceiveKeyCount.incrementAndGet();

            publishMsg(message.getBody(), ctx);

            return false;
        }

        return true;
    }

    private void publishMsg(String message, ChannelHandlerContext ctx) {
        //老版的用的单个HotKeyModel，新版用的数组
        List<HotKeyModel> models = FastJsonUtils.toList(message, HotKeyModel.class);
        long now = SystemClock.now();
        for (HotKeyModel model : models) {
            //白名单key不处理
            if (WhiteListHolder.contains(model.getKey())) {
                continue;
            }
            long timeOut = now - model.getCreateTime();
            if (timeOut > 1000) {
                logger.info("key timeout " + timeOut + ", from ip : " + NettyIpUtil.clientIp(ctx));
            }
            keyProducer.push(model, now);
        }

    }

}