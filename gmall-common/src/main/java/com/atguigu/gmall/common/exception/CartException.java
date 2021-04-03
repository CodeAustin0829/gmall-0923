package com.atguigu.gmall.common.exception;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/28
 */
public class CartException extends RuntimeException {
    public CartException() {
        super();
    }

    public CartException(String message) {
        super(message);
    }
}
