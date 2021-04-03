package com.atguigu.gmall.item.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Description 自定义线程池
 * @Author Austin
 * @Date 2021/3/25
 */
@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor(
            //在参数中读取配置文件application.yml中的配置
            @Value("${treadPool.corePoolSize}") Integer corePoolSize,
            @Value("${treadPool.maximumPoolSize}") Integer maximumPoolSize,
            @Value("${treadPool.keepAliveTime}") Integer keepAliveTime,
            @Value("${treadPool.blockQueueSize}") Integer blockQueueSize
    ) {
        return new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
                keepAliveTime, TimeUnit.SECONDS, new ArrayBlockingQueue<>(blockQueueSize));
    }

}
