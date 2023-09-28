package com.github.jesse.l2cache.example.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode// 特别注意：key为自定义DTO，重写equals()和hashcode()方法。目的：获取到缓存数据后，业务代码中可根据自定义DTO对象，找到其对应的缓存项。
public class GoodsPriceRevisionIdsReqDTO {

    private Integer tenantId;// 租户ID
    private Integer goodsGroupId;// 商品分组ID
    private Integer goodsId;// 商品ID

}
