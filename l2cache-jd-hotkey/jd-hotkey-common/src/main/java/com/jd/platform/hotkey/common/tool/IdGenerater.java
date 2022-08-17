package com.jd.platform.hotkey.common.tool;

import java.util.UUID;

/**
 * 给每个消息生成一个唯一id
 *
 * @author wuweifeng wrote on 2019-12-10
 * @version 1.0
 */
public class IdGenerater {
    public static String generateId() {
        String uuid = UUID.randomUUID().toString();
        return uuid.replace("-", "");
    }
}
