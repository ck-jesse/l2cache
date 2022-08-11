//package cn.weeget.hotkey.client.callback;
//
//import cn.weeget.hotkey.client.log.JdLogger;
//import cn.weeget.hotkey.common.model.HotKeyModel;
//import cn.weeget.hotkey.common.model.typeenum.KeyType;
//
///**
// * @author wuweifeng wrote on 2020-02-24
// * @version 1.0
// */
//public abstract class AbsReceiveNewKey implements ReceiveNewKeyListener {
//
//
//    @Override
//    public void newKey(HotKeyModel hotKeyModel) {
//        long now = System.currentTimeMillis();
//        //如果key到达时已经过去5秒了，记录一下。手工删除key时，没有CreateTime
//        if (hotKeyModel.getCreateTime() != 0 && Math.abs(now - hotKeyModel.getCreateTime()) > 1000) {
//            JdLogger.warn(getClass(), "the key comes too late : " + hotKeyModel.getKey() + " now " +
//                    + now + " keyCreateAt " +  hotKeyModel.getCreateTime());
//        }
//        if (hotKeyModel.isRemove()) {
//            deleteKey(hotKeyModel.getKey(), hotKeyModel.getKeyType(), hotKeyModel.getCreateTime());
//        } else {
//            //已经是热key了，又推过来同样的热key，做个日志记录，并刷新一下
//            if (JdHotKeyStore.isHot(hotKeyModel.getKey())) {
//                JdLogger.warn(getClass(), "receive repeat hot key ：" + hotKeyModel.getKey() + " at " + now);
//
//                //可能存在瞬间的该value过期的情况，所以要判空
////                ValueModel valueModel = JdHotKeyStore.getValueSimple(hotKeyModel.getKey());
////                if (valueModel != null) {
////                    valueModel.setCreateTime(System.currentTimeMillis());
////                } else {
////                    //else大概率走不到
////                    valueModel = ValueModel.defaultValue(hotKeyModel.getKey());
////                }
//                //已经是热key了，重新发过来了一样的，就将value初始化,客户端需要重新reset the value
//                //why my idea is so kakaka 怎么办，用baidu还卡不卡呢还是卡啊范围欧尼潍坊
//                ValueModel valueModel = ValueModel.defaultValue(hotKeyModel.getKey());
//                if (valueModel != null) {
//                    valueModel.setValue(valueModel);
//                    //刷新过期时间
//                    JdHotKeyStore.setValueDirectly(hotKeyModel.getKey(), valueModel);
//                }
//            } else {
//                //不是重复热key时，新建热key
//                addKey(hotKeyModel.getKey(), hotKeyModel.getKeyType(), hotKeyModel.getCreateTime());
//            }
//        }
//
//    }
//
//    abstract void addKey(String key, KeyType keyType, long createTime);
//
//    abstract void deleteKey(String key, KeyType keyType, long createTime);
//
//    protected void addNewKey(HotKeyModel hotKeyModel) {
//        ValueModel valueModel = ValueModel.defaultValue(hotKeyModel.getKey());
//        if (valueModel != null) {
//            valueModel.setValue(valueModel);
//            //刷新过期时间
//            JdHotKeyStore.setValueDirectly(hotKeyModel.getKey(), valueModel);
//        }
//    }
//}
