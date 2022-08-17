package com.jd.platform.hotkey.dashboard.util;

import java.io.Serializable;

/**
 * TwoTupleTwoTupleTwoTuple
 * @date 2020-02-24
 * @author wuweifeng wrote on 2020-02-24
 * @version 1.0
 */
public class TwoTuple<K, V> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * first
     */
    private K first;
    /**
     * second
     */
    private V second;

    /**
     * TwoTuple
     */
    public TwoTuple() {
    }

    /**
     * getFirst
     * @return first
     */
    public K getFirst() {
        return this.first;
    }

    public void setFirst(K first) {
        this.first = first;
    }

    /**
     * second
     * @return second
     */
    public V getSecond() {
        return this.second;
    }

    public void setSecond(V second) {
        this.second = second;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("[first = ").append(this.first).append(",").append("second = ").append("]");
        return strBuilder.toString();
    }
}
