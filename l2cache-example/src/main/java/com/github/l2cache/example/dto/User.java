package com.github.l2cache.example.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author chenck
 * @date 2020/5/7 20:27
 */
@Data
public class User implements Serializable {

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
