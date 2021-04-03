package com.atguigu.gmall.common.exception;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/25
 */
public class ItemException extends RuntimeException {
    public ItemException() {
        super();
    }

    public ItemException(String message) {
        super(message);
    }
}
