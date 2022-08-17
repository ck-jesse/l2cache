package com.jd.platform.hotkey.client.core.key;

import java.util.List;

/**
 * 对hotkey进行聚合
 * @author wuweifeng wrote on 2020-01-06
 * @version 1.0
 */
public interface IKeyCollector<T, V> {
    /**
     * 锁定后的返回值
     */
    List<V> lockAndGetResult();

    /**
     * 输入的参数
     */
    void collect(T t);

    void finishOnce();
}
