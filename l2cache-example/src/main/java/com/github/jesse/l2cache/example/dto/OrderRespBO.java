package com.github.jesse.l2cache.example.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderRespBO implements Serializable {

    private String orderId;
    private String userName;
    private Integer goodsNum;
}
