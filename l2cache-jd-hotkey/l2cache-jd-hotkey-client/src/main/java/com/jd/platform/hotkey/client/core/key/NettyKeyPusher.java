package com.jd.platform.hotkey.client.core.key;

import com.jd.platform.hotkey.client.Context;
import com.jd.platform.hotkey.client.core.worker.WorkerInfoHolder;
import com.jd.platform.hotkey.client.log.JdLogger;
import com.jd.platform.hotkey.common.model.HotKeyModel;
import com.jd.platform.hotkey.common.model.HotKeyMsg;
import com.jd.platform.hotkey.common.model.KeyCountModel;
import com.jd.platform.hotkey.common.model.MsgBuilder;
import com.jd.platform.hotkey.common.model.typeenum.MessageType;
import com.jd.platform.hotkey.common.tool.FastJsonUtils;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 将msg推送到netty的pusher
 * @author wuweifeng wrote on 2020-01-06
 * @version 1.0
 */
public class NettyKeyPusher implements IKeyPusher {

    @Override
    public void send(String appName, List<HotKeyModel> list) {
        //积攒了半秒的key集合，按照hash分发到不同的worker
        long now = System.currentTimeMillis();

        Map<Channel, List<HotKeyModel>> map = new HashMap<>();
        for(HotKeyModel model : list) {
            model.setCreateTime(now);
            Channel channel = WorkerInfoHolder.chooseChannel(model.getKey());
            if (channel == null) {
                continue;
            }

            List<HotKeyModel> newList = map.computeIfAbsent(channel, k -> new ArrayList<>());
            newList.add(model);
        }

        for (Channel channel : map.keySet()) {
            try {
                List<HotKeyModel> batch = map.get(channel);
                channel.writeAndFlush(MsgBuilder.buildByteBuf(new HotKeyMsg(MessageType.REQUEST_NEW_KEY, FastJsonUtils.convertObjectToJSON(batch)))).sync();
            } catch (Exception e) {
                try {
                    InetSocketAddress insocket = (InetSocketAddress) channel.remoteAddress();
                    JdLogger.error(getClass(),"flush error " + insocket.getAddress().getHostAddress());
                } catch (Exception ex) {
                    JdLogger.error(getClass(),"flush error");
                }

            }
        }

    }

    @Override
    public void sendCount(String appName, List<KeyCountModel> list) {
        //积攒了10秒的数量，按照hash分发到不同的worker
        long now = System.currentTimeMillis();
        Map<Channel, List<KeyCountModel>> map = new HashMap<>();
        for(KeyCountModel model : list) {
            model.setCreateTime(now);
            Channel channel = WorkerInfoHolder.chooseChannel(model.getRuleKey());
            if (channel == null) {
                continue;
            }

            List<KeyCountModel> newList = map.computeIfAbsent(channel, k -> new ArrayList<>());
            newList.add(model);
        }

        for (Channel channel : map.keySet()) {
            try {
                List<KeyCountModel> batch = map.get(channel);
                channel.writeAndFlush(MsgBuilder.buildByteBuf(new HotKeyMsg(Context.APP_NAME,
                        MessageType.REQUEST_HIT_COUNT, FastJsonUtils.convertObjectToJSON(batch)))).sync();
            } catch (Exception e) {
                try {
                    InetSocketAddress insocket = (InetSocketAddress) channel.remoteAddress();
                    JdLogger.error(getClass(),"flush error " + insocket.getAddress().getHostAddress());
                } catch (Exception ex) {
                    JdLogger.error(getClass(),"flush error");
                }

            }
        }
    }

}
