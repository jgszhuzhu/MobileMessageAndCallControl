package com.example.msgpushandcall.service;



public interface IComeMessage {

    /**
     * 短信来了
     */
    void  comeShortMessage(String msg);

    /**
     * 微信消息
     */
    void  comeWxMessage(String msg);

    /**
     * qq消息
     */
    void comeQQmessage(String msg);

}
