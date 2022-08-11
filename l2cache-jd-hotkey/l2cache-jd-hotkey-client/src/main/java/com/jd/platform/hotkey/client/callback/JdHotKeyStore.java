package com.jd.platform.hotkey.client.callback;

import com.jd.platform.hotkey.client.cache.CacheFactory;
import com.jd.platform.hotkey.client.cache.LocalCache;
import com.jd.platform.hotkey.client.core.key.HotKeyPusher;
import com.jd.platform.hotkey.client.core.key.KeyHandlerFactory;
import com.jd.platform.hotkey.client.core.key.KeyHotModel;
import com.jd.platform.hotkey.common.model.typeenum.KeyType;
import com.jd.platform.hotkey.common.tool.Constant;

/**
 * @author wuweifeng wrote on 2020-02-24
 * @version 1.0
 */
public class JdHotKeyStore {

    /**
     * 是否临近过期
     */
    private static boolean isNearExpire(ValueModel valueModel) {
        //判断是否过期时间小于1秒，小于1秒的话也发送
        if (valueModel == null) {
            return true;
        }
        return valueModel.getCreateTime() + valueModel.getDuration() - System.currentTimeMillis() <= 2000;
    }


    /**
     * 判断是否是key，如果不是，则发往netty
     */
    public static boolean isHotKey(String key) {
        try {
            if (!inRule(key)) {
                return false;
            }
            boolean isHot = isHot(key);
            if (!isHot) {
                HotKeyPusher.push(key, null);
            } else {
                ValueModel valueModel = getValueSimple(key);
                //判断是否过期时间小于1秒，小于1秒的话也发送
                if (isNearExpire(valueModel)) {
                    HotKeyPusher.push(key, null);
                }
            }

            //统计计数
            KeyHandlerFactory.getCounter().collect(new KeyHotModel(key, isHot));
            return isHot;
        } catch (Exception e) {
            return false;
        }

    }

    /**
     * 从本地caffeine取值
     */
    public static Object get(String key) {
        ValueModel value = getValueSimple(key);
        if (value == null) {
            return null;
        }
        Object object = value.getValue();
        //如果是默认值也返回null
        if (object instanceof Integer && Constant.MAGIC_NUMBER == (int) object) {
            return null;
        }
        return object;
    }

    /**
     * 判断是否是热key，如果是热key，则给value赋值
     */
    public static void smartSet(String key, Object value) {
        if (isHot(key)) {
            ValueModel valueModel = getValueSimple(key);
            if (valueModel == null) {
                return;
            }
            valueModel.setValue(value);
        }
    }

    /**
     * 获取value，如果value不存在则发往netty
     */
    public static Object getValue(String key, KeyType keyType) {
        try {
            //如果没有为该key配置规则，就不用上报key
            if (!inRule(key)) {
                return null;
            }
            Object userValue = null;

            ValueModel value = getValueSimple(key);

            if (value == null) {
                HotKeyPusher.push(key, keyType);
            } else {
                //临近过期了，也发
                if (isNearExpire(value)) {
                    HotKeyPusher.push(key, keyType);
                }
                Object object = value.getValue();
                //如果是默认值，也返回null
                if (object instanceof Integer && Constant.MAGIC_NUMBER == (int) object) {
                    userValue = null;
                } else {
                    userValue = object;
                }
            }

            //统计计数
            KeyHandlerFactory.getCounter().collect(new KeyHotModel(key, value != null));

            return userValue;
        } catch (Exception e) {
            return null;
        }

    }

    public static Object getValue(String key) {
        return getValue(key, null);
    }

    /**
     * 仅获取value，如果不存在也不上报热key
     */
    static ValueModel getValueSimple(String key) {
        Object object = getCache(key).get(key);
        if (object == null) {
            return null;
        }
        return (ValueModel) object;
    }

    /**
     * 纯粹的本地缓存，无需该key是热key
     */
    static void setValueDirectly(String key, Object value) {
        getCache(key).set(key, value);
    }

    /**
     * 删除某key，会通知整个集群删除
     */
    public static void remove(String key) {
        getCache(key).delete(key);
        HotKeyPusher.remove(key);
    }


    /**
     * 判断是否是热key。适用于只需要判断key，而不需要value的场景
     */
    static boolean isHot(String key) {
        return getValueSimple(key) != null;
    }

    private static LocalCache getCache(String key) {
        return CacheFactory.getNonNullCache(key);
    }

    /**
     * 判断这个key是否在被探测的规则范围内
     */
    private static boolean inRule(String key) {
        return CacheFactory.getCache(key) != null;
    }
}
