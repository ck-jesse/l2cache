package cn.weeget.hotkey.worker.netty.pusher;

import cn.weeget.hotkey.common.model.HotKeyModel;
import cn.weeget.hotkey.common.model.HotKeyMsg;
import cn.weeget.hotkey.common.model.MsgBuilder;
import cn.weeget.hotkey.common.model.typeenum.MessageType;
import cn.weeget.hotkey.common.tool.FastJsonUtils;
import cn.weeget.hotkey.worker.model.AppInfo;
import cn.weeget.hotkey.worker.netty.holder.ClientInfoHolder;
import io.netty.buffer.ByteBuf;
import org.springframework.stereotype.Component;

/**
 * 推送到各客户端服务器
 * @author wuweifeng wrote on 2020-02-24
 * @version 1.0
 */
@Component
public class AppServerPusher implements IPusher {

    /**
     * 给客户端推key信息
     */
    @Override
    public void push(HotKeyModel model) {
        for (AppInfo appInfo : ClientInfoHolder.apps) {
            if (model.getAppName().equals(appInfo.getAppName())) {

                HotKeyMsg hotKeyMsg = new HotKeyMsg(MessageType.RESPONSE_NEW_KEY, FastJsonUtils.convertObjectToJSON(model));
                String hotMsg = FastJsonUtils.convertObjectToJSON(hotKeyMsg);

                ByteBuf byteBuf = MsgBuilder.buildByteBuf(hotMsg);

                //整个app全部发送
                appInfo.groupPush(byteBuf);

                return;
            }
        }

    }

    @Override
    public void remove(HotKeyModel model) {
        push(model);
    }
}
