package com.github.jesse.l2cache.util;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 响应DTO，主要用于不同服务之间的数据传递
 * <p>
 * 建议用法：必须指定业务数据T的具体类型，如： ServiceResult<UserInfoDto>
 *
 * @author chenck
 * @date 2024/4/28 20:29
 */
@Data
@Accessors(chain = true)// 链式调用
public class ServiceResult<T> implements Serializable {

    public static final Integer SUCC = 0;
    public static final String SUCC_MSG = "ok";
    public static final Integer ERROR = 1;
    /**
     * 返回码
     */
    private Integer code = SUCC;

    /**
     * 描述信息
     */
    private String msg = SUCC_MSG;

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

    public ServiceResult(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 校验是否成功
     */
    @JsonIgnore
    public boolean isSucc() {
        return SUCC.equals(code);
    }

    private static <T> ServiceResult<T> result(T data, Integer code, String msg) {
        return new ServiceResult<T>().setCode(code).setMsg(msg).setData(data);
    }

    /**
     * 成功设置
     */
    public static <T> ServiceResult<T> succ() {
        return ServiceResult.result(null, SUCC, SUCC_MSG);
    }

    public static <T> ServiceResult<T> succ(T data) {
        return ServiceResult.result(data, SUCC, SUCC_MSG);
    }

    public static <T> ServiceResult<T> succ(String msg) {
        return ServiceResult.result(null, SUCC, msg);
    }

    public static <T> ServiceResult<T> succ(T data, String msg) {
        return ServiceResult.result(data, SUCC, msg);
    }

    public static <T> ServiceResult<T> succ(T data, Integer code, String msg) {
        return ServiceResult.result(data, code, msg);
    }


    /**
     * 失败设置
     */
    public static <T> ServiceResult<T> error(String msg) {
        return ServiceResult.result(null, ERROR, msg);
    }

    public static <T> ServiceResult<T> error(Integer code, String msg) {
        return ServiceResult.result(null, code, msg);
    }

    public static <T> ServiceResult<T> error(T data) {
        return ServiceResult.result(data, ERROR, "操作失败");
    }

    public static <T> ServiceResult<T> error(T data, String msg) {
        return ServiceResult.result(data, ERROR, msg);
    }

    public static <T> ServiceResult<T> error(T data, Integer code, String msg) {
        return ServiceResult.result(data, code, msg);
    }

}
