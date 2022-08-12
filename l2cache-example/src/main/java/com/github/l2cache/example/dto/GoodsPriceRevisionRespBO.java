package com.github.l2cache.example.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class GoodsPriceRevisionRespBO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer goodsPriceRevisionId;

    private Integer groupId;

    private Integer organizationId;

    private Integer goodsId;

    private Integer goodsGroupId;

    private Long addTime;

    private Long updateTime;

    private Integer state;
}
