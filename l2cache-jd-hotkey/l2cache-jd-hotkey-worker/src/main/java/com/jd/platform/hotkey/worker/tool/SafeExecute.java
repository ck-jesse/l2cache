package com.jd.platform.hotkey.worker.tool;

import java.util.function.Consumer;

/**
 * @author wuweifeng wrote on 2020-03-10
 * @version 1.0
 */
public class SafeExecute {

    public static <T> void execute(Consumer<T> consumer, String exMsg) {
        try {
            consumer.accept(null);
        } catch (Exception e) {
            System.out.println(exMsg);
        }
    }

    public static void main(String[] args) {
        execute(s -> doSome1(0), "exception");
    }

    public static void doSome(int i) {
        try {
            int j = 1 / i;
        } catch (Exception e) {
            System.out.println("exception");
        }
    }

    public static void doSome1(int i) {
        int j = 1 / i;
    }
}
