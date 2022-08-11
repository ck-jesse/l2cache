package com.jd.platform.hotkey.worker.netty.filter;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.StrUtil;
import com.jd.platform.hotkey.common.model.HotKeyMsg;
import com.jd.platform.hotkey.common.model.KeyCountModel;
import com.jd.platform.hotkey.common.model.typeenum.MessageType;
import com.jd.platform.hotkey.common.tool.FastJsonUtils;
import com.jd.platform.hotkey.common.tool.NettyIpUtil;
import com.jd.platform.hotkey.worker.counter.KeyCountItem;
import com.jd.platform.hotkey.worker.tool.InitConstant;
import com.jd.platform.hotkey.worker.counter.CounterConfig;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 对热key访问次数和总访问次数进行累计
 * @author wuweifeng wrote on 2020-6-24
 * @version 1.0
 */
@Component
@Order(4)
public class KeyCounterFilter implements INettyMsgFilter {

    private Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 该worker放到etcd worker目录的哪个app下
     */
    @Value("${etcd.workerPath}")
    private String workerPath;

    @Override
    public boolean chain(HotKeyMsg message, ChannelHandlerContext ctx) {
        if (MessageType.REQUEST_HIT_COUNT == message.getMessageType()) {
            //设置appName
            if (StrUtil.isEmpty(message.getAppName())) {
                message.setAppName(workerPath);
            }
            publishMsg(message.getAppName(), message.getBody(), ctx);

            return false;
        }

        return true;
    }

    private void publishMsg(String appName, String message, ChannelHandlerContext ctx) {
        //老版的用的单个HotKeyModel，新版用的数组
        List<KeyCountModel> models = FastJsonUtils.toList(message, KeyCountModel.class);
        long timeOut = SystemClock.now() - models.get(0).getCreateTime();
        //超时5秒以上的就不处理了，因为client是每10秒发送一次，所以最迟15秒以后的就不处理了
        if (timeOut > InitConstant.timeOut + 10000) {
            logger.warn("key count timeout " + timeOut + ", from ip : " + NettyIpUtil.clientIp(ctx));
            return;
        }
        //将收到的key放入延时队列，15秒后进行累加并发送
        try {
            CounterConfig.COUNTER_QUEUE.put(new KeyCountItem(appName, models.get(0).getCreateTime(), models));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}