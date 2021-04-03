package com.atguigu.gmall.index.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/22
 */
@Component
public class RedissonConfig {

    //初始化Redisson客户端
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        // 需要"rediss://"来启用SSL连接，否则报错
        config.useSingleServer().setAddress("redis://192.168.86.168:6379");
        return Redisson.create(config);

    }
}
