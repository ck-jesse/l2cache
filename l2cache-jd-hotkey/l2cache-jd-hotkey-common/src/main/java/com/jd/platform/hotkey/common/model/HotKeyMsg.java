package com.jd.platform.hotkey.common.model;

import com.jd.platform.hotkey.common.model.typeenum.MessageType;

/**
 * netty通信消息
 * @author wuweifeng wrote on 2020-01-06
 * @version 1.0
 */
public class HotKeyMsg {
    private int magicNumber;

    private String appName;

    private MessageType messageType;

    private String body;

    public HotKeyMsg(MessageType messageType, String body) {
        this(null, messageType, body);
    }

    public HotKeyMsg(String appName, MessageType messageType, String body) {
        this.appName = appName;
        this.messageType = messageType;
        this.body = body;
    }

    public HotKeyMsg() {
    }

    @Override
    public String toString() {
        return "HotKeyMsg{" +
                "magicNumber=" + magicNumber +
                ", messageType=" + messageType +
                ", body='" + body + '\'' +
                '}';
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public int getMagicNumber() {
        return magicNumber;
    }

    public void setMagicNumber(int magicNumber) {
        this.magicNumber = magicNumber;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
