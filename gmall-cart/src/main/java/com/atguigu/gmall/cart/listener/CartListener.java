package com.atguigu.gmall.cart.listener;

import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.pms.entity.SpuEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/29
 */
public class CartListener {


    @Autowired
    private GmallPmsClient gmallPmsClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String PRICE_PREFIX = "cart:info:"; // 比价的价格前缀

    /*
    1. 第一步：绑定队列
    在消息消费者中创建一个监听类，
    创建一个绑定交换机、队列
     */
    @RabbitListener(bindings = @QueueBinding(
            //（1）绑定队列，在程序中，交换机、消息默认是持久化的，所以只需要对队列进行持久化，就能实现将消息持久化到硬盘中
            value = @Queue(value = "CART_PRICE_EXCHANGE", durable = "true"),
            //（2）绑定交换机，交换机名称，需要跟生产者的一致；还需要忽略声明异常，因为交换机只有一个，当多个微服务共用一个交换机时，有可能发生声明异常
            exchange = @Exchange(value = "PMS_ITEM_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            //（3）通配模型的路由键
            key = {"item.update"}
    ))
    public void listener(Long spuId, Channel channel, Message message) throws IOException {
        //（1）当spuId为空的时候，cart微服务直接消费掉垃圾消息，然后结束当前方法
        if (spuId == null) {
            //手动确认模式：确认消息
            // 第一参数：固定写法，第二个参数：是否批量确认，一般设置为false
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }

        //（2）当spuId不为空的时候，根据spuId查询spuEntity
        ResponseVo<SpuEntity> spuEntityResponseVo = this.gmallPmsClient.querySpuById(spuId);
        //从ResponseVo的属性data中获取SpuEntity
        SpuEntity spuEntity = spuEntityResponseVo.getData();


        /*
        （3）当spuEntity也不为空时，远程调用，查询出spu下面的所有的sku
         */
        ResponseVo<List<SkuEntity>> skuResponseVo = this.gmallPmsClient.querySkuBySpuId(spuId);
        List<SkuEntity> skuEntities = skuResponseVo.getData();
        // 对获取到的集合skuEntities进行判断，如果不为空，则对skuEntities进行遍历
        if (!CollectionUtils.isEmpty(skuEntities)) {
            skuEntities.forEach(skuEntity -> {
                // 判断缓存中是否含有所要更新价格的商品
                if (this.stringRedisTemplate.hasKey(PRICE_PREFIX + skuEntity.getId())) {
                    // 如果含有，则更新redis中的价格，key时skuId，value是要更新的价格
                    this.stringRedisTemplate.opsForValue().set(
                            PRICE_PREFIX + skuEntity.getId(), skuEntity.getPrice().toString());
                }
            });
        }
    }

}
