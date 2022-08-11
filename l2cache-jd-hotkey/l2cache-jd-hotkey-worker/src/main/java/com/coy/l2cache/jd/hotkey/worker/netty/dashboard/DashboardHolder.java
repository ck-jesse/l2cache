package com.coy.l2cache.jd.hotkey.worker.netty.dashboard;

import com.coy.l2cache.jd.hotkey.common.model.HotKeyMsg;
import com.coy.l2cache.jd.hotkey.common.model.MsgBuilder;
import com.coy.l2cache.jd.hotkey.common.model.typeenum.MessageType;
import io.netty.channel.Channel;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-08-31
 */
public class DashboardHolder {
    /**
     * 是否连上了dashboard
     */
    public static boolean hasConnected = false;
    /**
     * channel
     */
    public static Channel channel = null;

    public static void flushToDashboard(String message) {
        channel.writeAndFlush(MsgBuilder.buildByteBuf(new HotKeyMsg(MessageType.REQUEST_HOT_KEY, message)));
    }
}