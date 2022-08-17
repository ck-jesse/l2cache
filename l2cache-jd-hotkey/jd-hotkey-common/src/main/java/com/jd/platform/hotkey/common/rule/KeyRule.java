package com.jd.platform.hotkey.common.rule;

/**
 * @author wuweifeng wrote on 2020-02-26
 * @version 1.0
 */
public class KeyRule {
    /**
     * key的前缀，也可以完全和key相同。为"*"时代表通配符
     */
    private String key;
    /**
     * 是否是前缀，true是前缀
     */
    private boolean prefix;
    /**
     * 间隔时间（秒）
     */
    private int interval;
    /**
     * 累计数量
     */
    private int threshold;
    /**
     * 变热key后，本地、etcd缓存它多久。单位（秒），默认60
     */
    private int duration;
    /**
     * 描述
     */
    private String desc;

    public KeyRule(String key, boolean prefix, int interval, int threshold, int duration) {
        this(key, prefix, interval, threshold, duration, null);
    }

    public KeyRule(String key, boolean prefix, int interval, int threshold, int duration, String desc) {
        this.key = key;
        this.prefix = prefix;
        this.interval = interval;
        this.threshold = threshold;
        this.duration = duration;
        this.desc = desc;
    }

    public String getKey() {
        return key;
    }

    public boolean isPrefix() {
        return prefix;
    }

    public String getDesc() {
        return desc;
    }

    public int getInterval() {
        return interval;
    }

    public int getThreshold() {
        return threshold;
    }

    public int getDuration() {
        return duration;
    }

    public static class Builder {
        /**
         * key的前缀，也可以完全和key相同
         */
        private String key;
        /**
         * 是否是前缀，true是前缀
         */
        private boolean prefix;
        /**
         * 间隔时间（秒）
         */
        private int interval;
        /**
         * 累计数量
         */
        private int threshold;
        /**
         * 变热key后，本地、etcd缓存它多久。默认1分钟
         */
        private int duration;
        /**
         * 描述
         */
        private String desc;

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder prefix(boolean prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder desc(String desc) {
            this.desc = desc;
            return this;
        }

        public Builder interval(int interval) {
            if (interval < 1) {
                interval = 1;
            }
            this.interval = interval;
            return this;
        }

        public Builder threshold(int threshold) {
            this.threshold = threshold;
            return this;
        }

        public Builder duration(int duration) {
            if (duration < 1) {
                duration = 1;
            }
            this.duration = duration;
            return this;
        }

        public KeyRule build() {
            return new KeyRule(key, prefix, interval, threshold, duration, desc);
        }
    }

    @Override
    public String toString() {
        return "DefaultKeyRule{" +
                "key='" + key + '\'' +
                ", prefix=" + prefix +
                ", interval=" + interval +
                ", threshold=" + threshold +
                ", duration=" + duration +
                '}';
    }

}
