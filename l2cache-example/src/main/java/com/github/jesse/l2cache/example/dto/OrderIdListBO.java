package com.github.jesse.l2cache.example.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrderIdListBO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> orderIdList;

    public void addOrderId(String orderId) {
        if (null == orderIdList) {
            orderIdList = new ArrayList<>();
        }
        orderIdList.add(orderId);
    }
}
