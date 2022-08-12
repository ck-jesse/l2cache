package com.github.l2cache.util;


import lombok.Data;

import java.io.Serializable;

/**
 * 响应DTO，主要用于不同服务之间的数据传递
 *
 * @author chenck
 * @date 2019/6/28 16:15
 */
@Data
public class ServiceResult<T> implements Serializable {

    public static final String SUCC = "0";
    public static final String SUCC_MSG = "ok";
    public static final String FAIL = "1";

    /**
     * 返回码
     */
    private String code = SUCC;

    /**
     * 描述信息
     */
    private String msg = "ok";

    /**
     * 返回业务数据
     */
    private T data;


    // 定义构造函数，方便使用
    public ServiceResult() {
    }

    public ServiceResult(String msg) {
        this.msg = msg;
    }

    public ServiceResult(T data) {
        this.data = data;
    }

    public ServiceResult(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public ServiceResult<T> setData(T data) {
        this.data = data;
        return this;
    }

    private static <T> ServiceResult<T> result(T data, String code, String msg) {
        ServiceResult responseResult = new ServiceResult(code, msg);
        responseResult.setData(data);
        if (null != msg) {
            responseResult.setMsg(msg);
        }
        return responseResult;
    }

    /**
     * 成功设置
     */
    public static ServiceResult succ() {
        return ServiceResult.result(null, SUCC, SUCC_MSG);
    }

    public static <T> ServiceResult<T> succ(T data) {
        return ServiceResult.result(data, SUCC, SUCC_MSG);
    }

    public static <T> ServiceResult<T> succ(T data, String msg) {
        return ServiceResult.result(data, SUCC, msg);
    }

    public static <T> ServiceResult<T> succ(T data, String code, String msg) {
        return ServiceResult.result(data, code, msg);
    }

    /**
     * 失败设置
     */
    public static ServiceResult error(String msg) {
        return ServiceResult.result(null, FAIL, msg);
    }

    public static ServiceResult error(String code, String msg) {
        return ServiceResult.result(null, code, msg);
    }

    public static <T> ServiceResult<T> error(T data, String msg) {
        return ServiceResult.result(data, FAIL, msg);
    }

    public static <T> ServiceResult<T> error(T data, String code, String msg) {
        return ServiceResult.result(data, code, msg);
    }

    /**
     * 校验是否成功
     */
    public boolean isSucc() {
        return SUCC.equals(code);
    }

}
