package com.atguigu.gmall.common.exception;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/31
 */
public class OrderException extends RuntimeException {

    public OrderException() {
        super();
    }

    public OrderException(String message) {
        super(message);
    }
}