package com.github.jesse.l2cache.example.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class BrandRespBO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer brandId;

    private Integer groupId;

    private String brandName;

    private String brandNumber;

    private String description;

    private Integer state;

}
