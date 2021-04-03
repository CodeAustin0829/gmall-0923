package com.atguigu.gmall.common.exception;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/26
 */
public class UserException extends RuntimeException {
    public UserException() {
        super();
    }

    public UserException(String message) {
        super(message);
    }
}
