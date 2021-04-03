package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
    private static final String pubKeyPath = "D:\\Developer_tools\\repos\\IdeaPros\\gmall\\rsa\\rsa.pub";
    private static final String priKeyPath = "D:\\Developer_tools\\repos\\IdeaPros\\gmall\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        /**
         * 测试注解@Test导这个包：
         * import org.junit.Test;
         * 执行会报错：
         * java.lang.IllegalArgumentException: Key argument cannot be null.
         * 要导成这个包：
         * import org.junit.jupiter.api.Test;
         */
        Map<String, Object> map = new HashMap<>();
        map.put("id", "829");
        map.put("username", "libifu");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 5);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjgyOSIsInVzZXJuYW1lIjoibGliaWZ1IiwiZXhwIjoxNjE2NzQ1MzUxfQ.UaQKqp9mQ3uE_c2kNnvk1rkR7kGlJeRSrIEBH3bh2NTh_QOdI0yfDMgEmaG9qlyI4w8_YY5gxme2d8WvueVhXOwOFGO7F4tsvPdTwHlakuNXp62iQRgtAQqU-RYImdzHTQbvLNwtCXqwUsPnyCmEZBW9pYhNYVvNkWpAYu0XWQftwDcisseBnNd4b907EIBefBUPUGjnlsublTfwJlPx6Bl6LDunzzxlDrjw1htInZfD9WTA66heoB-foAgc1-yxqSgHR0fOncLPS5_-BKZBs1EXR9JsAcqO1q6yiMwU0dgfXloqYQQxkPq0BTY6jkAB-nbODTW9py9uOmb7-rKV0w";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}