package com.atguigu.gmall.cart.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
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
    private String cookieName;
    private String userKey;
    private Integer expireTime;

    // 声明公钥、私钥对象
    private PublicKey publicKey;

    /**
     * 该方法在构造方法执行之后执行
     * 当JwtProperties类被加载后，被 @PostConstruct 注解的方法就被执行
     * 也就是将公钥、私钥对象进行初始化
     * 这样别的地方需要使用公钥和私钥的时候，就已经是读取好的公私钥对象
     */
    @PostConstruct
    public void init() {
        try {
            // 读取公钥对象，赋值给当前的公钥对象
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            log.error("生成公钥和私钥出错");
            e.printStackTrace();
        }
    }


}
