package com.atguigu.gmall.auth.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * @Description 从配置文件读取jwt配置的配置类
 * @Author Austin
 * @Date 2021/3/26
 */
@Data
@Slf4j
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

    private String pubKeyPath;
    private String priKeyPath;
    private String secret;
    private Integer expire;
    private String cookieName;
    private String unick;

    // 声明公钥、私钥对象
    private PublicKey publicKey;
    private PrivateKey privateKey;

    /**
     * 该方法在构造方法执行之后执行
     * 当JwtProperties类被加载后，被 @PostConstruct 注解的方法就被执行
     * 也就是将公钥、私钥对象进行初始化
     * 这样别的地方需要使用公钥和私钥的时候，就已经是读取好的公私钥对象
     */
    @PostConstruct
    public void init() {
        try {
            // 从文件中获取公私钥
            File pubFile = new File(pubKeyPath);
            File priFile = new File(priKeyPath);

            // 判断秘钥是否都存在，如果有一个不存在，都要重新生成
            // exists():测试此抽象路径名表示的文件或目录是否存在
            if (!pubFile.exists() || !priFile.exists()) {
                // 如果不存在，生成公私钥
                RsaUtils.generateKey(pubKeyPath, priKeyPath, secret);
            }

            // 读取公私钥对象，赋值给当前的公私钥对象
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
            this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
        } catch (Exception e) {
            log.error("生成公钥和私钥出错");
            e.printStackTrace();
        }
    }


}
