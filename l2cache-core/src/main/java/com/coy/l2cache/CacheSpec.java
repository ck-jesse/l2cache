package com.coy.l2cache;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 缓存配置
 * 注：从一级缓存中解析出来的各个维度的值，可用于二级缓存中设置对应的参数。如缓存过期时间、最大缓存数等。
 *
 * @author chenck
 * @date 2020/9/2 16:18
 */
@Getter
@Setter
@Accessors(chain = true)
public class CacheSpec {

    /**
     * 缓存过期时间(ms)
     */
    private long expireTime;

    /**
     * 最大缓存数，以便剔除多余元素
     * 注：注意如果与一级缓存（如：caffeine）中最大数量大小不一致，会出现一级缓存和二级缓存中缓存数量不一致，所以建议设置为一致减少不必要的歧义。
     */
    private int maxSize;
}
