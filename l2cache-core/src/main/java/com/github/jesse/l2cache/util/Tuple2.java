package com.github.jesse.l2cache.util;

/**
 * 二元组类型
 *
 * @author chenck
 * @date 2020/4/22 11:35
 */
public class Tuple2<T1, T2> {
    private T1 t1;
    private T2 t2;

    public Tuple2(T1 t1, T2 t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    public T1 getT1() {
        return t1;
    }

    public void setT1(T1 t1) {
        this.t1 = t1;
    }

    public T2 getT2() {
        return t2;
    }

    public void setT2(T2 t2) {
        this.t2 = t2;
    }

    /**
     * 快捷入口
     */
    public static <C1, C2> Tuple2<C1, C2> of(C1 c1, C2 c2) {
        return new Tuple2(c1, c2);
    }

    /**
     * 交换
     */
    public Tuple2<T2, T1> swap() {
        return new Tuple2(this.t2, this.t1);
    }

    @Override
    public String toString() {
        return "Tuple2{r1=" + this.t1 + ", r2=" + this.t2 + '}';
    }
}
