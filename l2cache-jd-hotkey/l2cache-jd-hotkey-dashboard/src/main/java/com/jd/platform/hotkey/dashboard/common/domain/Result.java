package com.jd.platform.hotkey.dashboard.common.domain;

import com.jd.platform.hotkey.dashboard.common.eunm.ResultEnum;

import java.io.Serializable;


public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String msg;

    private T data;

    public Result() {
    }

    public Result(Integer code, T data) {
        this.code = code;
        this.data = data;
    }

    public Result(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Result(ResultEnum resultEnum) {
        this.code = resultEnum.getCode();
        this.msg = resultEnum.getMsg();
    }

    public static Result success(){
        return new Result<>(ResultEnum.SUCCESS);
    }

    public static Result success(Object data){
        return new Result<>(200,data);
    }

    public static Result fail(){
        return new Result<>(ResultEnum.NO_CHANGE);
    }

    public static Result error(int code, String msg){
        return new Result<>(code,msg);
    }

    public static Result error(){
        return new Result<>(ResultEnum.BIZ_ERROR);
    }

    public static Result error(ResultEnum result){
        return new Result<>(result);
    }


    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Result{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }
}
