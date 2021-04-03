package com.atguigu.gmall.search.listener;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsFeignClient;
import com.atguigu.gmall.search.feign.GmallWmsFeignClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.search.vo.SearchAttrValueVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description 监听pms发送到MQ的消息
 * @Author Austin
 * @Date 2021/3/20
 */
@Component
public class SpuInfoListener {

    @Autowired
    private GoodsRepository goodsRepository;
    /**
     * 自动注入 GmallPmsFeignClient、GmallWmsFeignClient
     * 远程调用 pms、wms 微服务
     */
    @Autowired
    private GmallPmsFeignClient gmallPmsFeignClient;
    @Autowired
    private GmallWmsFeignClient gmallWmsFeignClient;

    /*
    1. 第一步：绑定队列
    在消息消费者中创建一个监听类，
    创建一个绑定交换机、队列
     */
    @RabbitListener(bindings = @QueueBinding(
            //（1）绑定队列，在程序中，交换机、消息默认是持久化的，所以只需要对队列进行持久化，就能实现将消息持久化到硬盘中
            value = @Queue(value = "SEARCH_ITEM_EXCHANGE", durable = "true"),
            //（2）绑定交换机，交换机名称，需要跟生产者的一致；还需要忽略声明异常，因为交换机只有一个，当多个微服务共用一个交换机时，有可能发生声明异常
            exchange = @Exchange(value = "PMS_ITEM_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            //（3）通配模型的路由键
            key = {"item.insert"}
    ))
    public void listener(Long spuId, Channel channel, Message message) throws IOException {
        //（1）当spuId为空的时候，search微服务直接消费消息，然后结束当前方法
        if (spuId == null) {
            //手动确认模式：确认消息
            // 第一参数：固定写法，第二个参数：是否批量确认，一般设置为false
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }

        //（2）当spuId不为空的时候，根据spuId查询spuEntity
        ResponseVo<SpuEntity> spuEntityResponseVo = this.gmallPmsFeignClient.querySpuById(spuId);
        //从ResponseVo的属性data中获取SpuEntity
        SpuEntity spuEntity = spuEntityResponseVo.getData();

        /*
        （3）当spuEntity也不为空时，search微服务操作ES，进行更新数据的操作；更新完数据，第三步再手动确认消息即可
        复制 GmallSearchApplicationTests 类中的保存遍历spu保存sku的代码即可
         */
        // 2.3 每遍历一次，就根据SpuId查询一次sku
        ResponseVo<List<SkuEntity>> skuResponseVo = this.gmallPmsFeignClient.querySkuBySpuId(spuEntity.getId());
        // 2.4 从ResponseVo中获取List<SkuEntity>，已封装在其data属性中
        List<SkuEntity> skuEntityList = skuResponseVo.getData();
        // 2.5 对获取到的集合skuEntityList进行判断，如果不为空，则进行下一步操作
        if (!CollectionUtils.isEmpty(skuEntityList)) {
                    /*
                    2.11 设置Goods对应的属性封装的数据:聚合字段之品牌和分类
                    在pms_spu表和pms_sku表中都可以根据品牌id、分类id查询出品牌和分类
                    但为了提供查询性能，缩减查询次数，放在pms_spu进行查询
                    因为spu的数据比sku的数据要少很多
                    先查询出
                    品牌对象brandEntity
                    分类对象categoryEntity
                    再放在saveAll()进行保存
                     */
            ResponseVo<BrandEntity> brandEntityResponseVo = this.gmallPmsFeignClient.queryBrandById(spuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            ResponseVo<CategoryEntity> categoryEntityResponseVo = this.gmallPmsFeignClient.queryCategoryById(spuEntity.getCategoryId());
            CategoryEntity categoryEntity = categoryEntityResponseVo.getData();


            /**
             * 3.第三步：将sku转换为goods，导入到ES索引库中
             */
            // 2.7 ElasticsearchRepository提供了一个保存集合的方法saveAll()
            this.goodsRepository.saveAll(
                    // 2.6 使用stream表达式，将sku转换为goods
                    skuEntityList.stream().map(skuEntity -> {
                        // 2.7 创建一个Goods对象，接收转换
                        Goods goods = new Goods();

                                /*
                                2.9 设置Goods对应的属性封装的数据:商品列表字段
                                底层方法：doubleValue()，将 BigDecimal 类型转换为 double 类型
                                 */
                        goods.setSkuId(skuEntity.getId());
                        goods.setTitle(skuEntity.getTitle());
                        goods.setSubTitle(skuEntity.getSubtitle());
                        goods.setPrice(skuEntity.getPrice().doubleValue());
                        goods.setDefaultImage(skuEntity.getDefaultImage());

                                /*
                                2.10 设置Goods对应的属性封装的数据:排序字段
                                ① 新品字段在spu中，上面已经进行了远程调用
                                ② 销量、库存需要远程调用wms，根据skuId来进行查询
                                ③ 得到一个ResponseVo<List<WareSkuEntity>>对象
                                ④ 从ResponseVo<List<WareSkuEntity>>中，获取List<WareSkuEntity>
                                ⑤ 对获取到的List<WareSkuEntity>进行判空，不为空才进行赋值操作
                                ⑥ 库存赋值
                                  当数据库表wms_ware_sku中的 stock(库存数) 与 stock_locked(库存) 相减大于0时，
                                  说明真的有没有被锁定的库存，这样才不会出现超卖现象
                                  使用stream表达式的方法anyMatch()
                                  【anyMatch(Predicate p) 传入一个断言型函数，对流中所有的元素进行判断
                                  只要有一个满足条件就返回true，都不满足返回false】
                                ⑦ 销量赋值
                                  取的每个仓库的销量之和，使用stream表达式的方法reduce()来求和
                                  先获取销量的集合，再进行求和
                                  wareSkuEntity->wareSkuEntity.getSales()【对象调方法】
                                  也可以写成
                                  WareSkuEntity::getSales【使用类直接调方法】
                                  get()是底层Optional类提供的方法，可以获取到值
                                 */
                        goods.setCreateTime(spuEntity.getCreateTime());
                        ResponseVo<List<WareSkuEntity>> wareSkuEntityResponseVo = this.gmallWmsFeignClient.queryWareSkuBySkuId(skuEntity.getId());
                        List<WareSkuEntity> wareSkuEntityList = wareSkuEntityResponseVo.getData();
                        if (!CollectionUtils.isEmpty(wareSkuEntityList)) {
                            boolean store = wareSkuEntityList.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0);
                            goods.setStore(store);
                            Long sales = wareSkuEntityList.stream().map(WareSkuEntity::getSales).reduce((a, b) -> a + b).get();
                            goods.setSales(sales);
                        }

                                /*
                                2.11 设置Goods对应的属性封装的数据:聚合字段之品牌和分类
                                在pms_spu表和pms_sku表中都可以根据品牌id、分类id查询出品牌和分类
                                但为了提供查询性能，缩减查询次数，放在pms_spu进行查询
                                因为spu的数据比sku的数据要少很多
                                先查询出
                                品牌对象brandEntity
                                分类对象categoryEntity
                                再放在saveAll()进行保存
                                 */
                        if (brandEntity != null) {
                            goods.setBrandId(brandEntity.getId());
                            goods.setBrandName(brandEntity.getName());
                            goods.setLogo(brandEntity.getLogo());
                        }
                        if (categoryEntity != null) {
                            goods.setCategoryId(categoryEntity.getId());
                            goods.setCategoryName(categoryEntity.getName());
                        }

                                /*
                                2.12 设置Goods对应的属性封装的数据:聚合字段之规格参数
                                ① 查询检索类型为销售类型的检索属性，也就是查pms_sku_attr_value
                                  得到一个List<SkuAttrValueEntity>
                                  将其转换goods的属性List<SearchAttrValueVo>
                                ② 查询检索类型为基本类型的检索属性，也就是查pms_spu_attr_value
                                  得到一个List<SpuAttrValueEntity>
                                  将其转换goods的属性List<SearchAttrValueVo>
                                ③ 将List<SearchAttrValueVo>设置到goods的属性searchAttrs中
                                 */
                        //① 创建一个大的集合
                        List<SearchAttrValueVo> searchAttrValueVoList = new ArrayList<>();

                        //② 查询得到List<SkuAttrValueEntity>，赋值到大集合中
                        ResponseVo<List<SkuAttrValueEntity>> skuAttrValueEntityResponseVo =
                                this.gmallPmsFeignClient.querySkuAttrValueByCidAndSkuId(skuEntity.getCategoryId(), skuEntity.getId());
                        List<SkuAttrValueEntity> skuAttrValueEntityList = skuAttrValueEntityResponseVo.getData();
                        if (!CollectionUtils.isEmpty(skuAttrValueEntityList)) {
                            //将List<SearchAttrValueVo> list 批量添加到 List<SearchAttrValueVo> searchAttrValueVoList中
                            searchAttrValueVoList.addAll(skuAttrValueEntityList.stream().map(skuAttrValueEntity -> {
                                SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                                //searchAttrValueVo与skuAttrValueEntity的属性一致，可以使用属性对拷
                                BeanUtils.copyProperties(skuAttrValueEntity, searchAttrValueVo);
                                return searchAttrValueVo;
                            }).collect(Collectors.toList()));
                        }

                        //③ 查询得到List<SpuAttrValueEntity>，赋值到大集合中
                        ResponseVo<List<SpuAttrValueEntity>> spuAttrValueEntityResponseVo =
                                this.gmallPmsFeignClient.querySpuAttrValueByCidAndSpuId(spuEntity.getCategoryId(), spuEntity.getId());
                        List<SpuAttrValueEntity> spuAttrValueEntityList = spuAttrValueEntityResponseVo.getData();
                        if (!CollectionUtils.isEmpty(spuAttrValueEntityList)) {
                            searchAttrValueVoList.addAll(spuAttrValueEntityList.stream().map(spuAttrValueEntity -> {
                                SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                                BeanUtils.copyProperties(spuAttrValueEntity, searchAttrValueVo);
                                return searchAttrValueVo;
                            }).collect(Collectors.toList()));
                        }
                        goods.setSearchAttrs(searchAttrValueVoList);

                        // 2.8 将goods返回到stream流中，也就是放入到新的List集合中
                        return goods;
                    }).collect(Collectors.toList()));
        }
        //（4）当更新数据成功，手动确认消息
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
