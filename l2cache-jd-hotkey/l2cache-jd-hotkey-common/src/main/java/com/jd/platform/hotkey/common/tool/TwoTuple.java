package com.jd.platform.hotkey.common.tool;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-06-24
 */
public class TwoTuple<A, B> {

    public final A first;

    public final B second;

    public TwoTuple(A a, B b){
        first = a;
        second = b;
    }

    public String toString(){
        return "(" + first + ", " + second + ")";
    }

}