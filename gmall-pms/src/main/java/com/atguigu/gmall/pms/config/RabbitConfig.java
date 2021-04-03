package com.atguigu.gmall.pms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import javax.annotation.PostConstruct;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/20
 */
//（1）第一步：加 @Configuration 注解，将RabbitMQConfig声明为配置类，加入到容器中，主启动类运行时会加载
@Configuration
@Slf4j
public class RabbitConfig {

    /*
    （2）第二步：
    根据springboot自动配置原理，自动注入 RabbitTemplate
    RabbitTemplate:消息模板，这是springboot整合rabbitmq提供的消息模板，是进行发送消息的关键类。
     */
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * （3）第三步：生产者确认机制
     * 由于是使用RabbitTemplate来发布消息的，所以也需要在配置类中，告诉RabbitTemplate是否有发送成功
     * ① setConfirmCallback方法:确认消息是否到达交换机的回调方法，不管消息是否到达交换机，该回调方法都会被执行
     * ② setReturnCallback方法：确认消息是否到达队列的回调方法，只有消息没有到达队列，才执行此回调方法
     * ------------------------------------------------------------------------------------------------
     * 当启动类启动后，会扫描@Configuration[底层也是@Component]所注解的类RabbitMQConfig，调用无参构造器方法
     * 将RabbitMQConfig类进行初始化，将RabbitMQConfig对象注入到IOC容器中
     * 执行完无参构造器方法后
     * 立马会调用@PostConstruct所注解的方法init()方法，进行初始化
     * 这样就可以将两个回调方法setConfirmCallback、setReturnCallback设置给RabbitTemplate
     */
    @PostConstruct
    public void init() {
        //确认消息是否到达交换机的回调方法，不管消息是否到达交换机，该回调方法都会被执行
        this.rabbitTemplate.setConfirmCallback((@Nullable CorrelationData correlationData, boolean ack, @Nullable String cause) -> {
            if (ack) {
                //当消息成功到达交换机
                log.info("消息成功到达交换机！");
            } else {
                log.error("消息未到达交换机，失败原因：{}", cause);
            }
        });

        // 确认消息是否到达队列的回调方法，只有消息没有到达队列，才执行此回调方法
        this.rabbitTemplate.setReturnCallback((Message message, int replyCode, String replyText, String exchange, String routingKey) -> {
            log.error("消息没有到达消息队列，交换机：{}，路由键：{}，消息内容：{}", exchange, routingKey, new String(message.getBody()));
        });
    }

}
