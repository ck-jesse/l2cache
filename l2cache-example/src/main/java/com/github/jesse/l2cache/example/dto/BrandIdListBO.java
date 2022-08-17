package com.github.jesse.l2cache.example.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class BrandIdListBO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Integer> brandIdList;

    public void addBrandId(Integer brandId) {
        if (null == brandIdList) {
            brandIdList = new ArrayList<>();
        }
        brandIdList.add(brandId);
    }
}
