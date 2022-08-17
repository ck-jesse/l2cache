package com.jd.platform.hotkey.dashboard.common.domain;

import java.util.Date;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-09-02
 */
public interface IRecord {
    /**
     * appName + "/" + key
     */
    String appNameKey();

    /**
     * 手工添加的是时间戳13位，worker传过来的是uuid
     */
    String value();

    /**
     * 0插入，1删除
     */
    int type();

    Date createTime();
}
