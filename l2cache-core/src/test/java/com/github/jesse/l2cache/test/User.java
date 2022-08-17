package com.github.jesse.l2cache.test;

import lombok.Data;

/**
 * @author chenck
 * @date 2020/5/7 20:27
 */
@Data
public class User {

    public User() {

    }

    public User(String name, String addr) {
        this.name = name;
        this.addr = addr;
    }

    private String name;
    private String addr;
    private long currTime = System.currentTimeMillis();
}
