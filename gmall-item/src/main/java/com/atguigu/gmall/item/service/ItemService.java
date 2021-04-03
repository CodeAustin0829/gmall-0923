package com.atguigu.gmall.item.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.entity.vo.ItemGroupVo;
import com.atguigu.gmall.pms.entity.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/25
 */
@Service
@Slf4j
public class ItemService {

    @Autowired
    private GmallPmsClient gmallPmsClient;
    @Autowired
    private GmallSmsClient gmallSmsClient;
    @Autowired
    private GmallWmsClient gmallWmsClient;

    /**
     * 注入自定义的线程池
     */
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    /**
     * 注入Thymeleaf页面静态化的模板引擎类
     */
    @Autowired
    private TemplateEngine templateEngine;

    /**
     * 加载商品详情页
     *
     * @param skuId
     * @return
     */
    public ItemVo loadData(Long skuId) throws Exception {

        ItemVo itemVo = new ItemVo();

        /**
         * 使用CompletableFuture异步编排
         * 优化加载商品详情页
         */
        CompletableFuture<SkuEntity> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            // 1.商品详情页接口一：根据skuId，查询sku信息
            ResponseVo<SkuEntity> skuEntityResponseVo = this.gmallPmsClient.querySkuById(skuId);
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            // 只要通过SkuEntity来调用【也就是“.”】，就需要判空，否则就可能发生空指针异常
            /*
            在最后的组合方法的exceptionally进行处理
            if (skuEntity == null) {
                throw new Exception("访问的页面不存在!");
            }
            */
            // 为ItemVo的sku属性赋值
            itemVo.setSkuId(skuId);
            itemVo.setTitle(skuEntity.getTitle());
            itemVo.setSubTitle(skuEntity.getSubtitle());
            itemVo.setPrice(skuEntity.getPrice());
            itemVo.setWeight(skuEntity.getWeight());
            itemVo.setDefaultImage(skuEntity.getDefaultImage());
            //返回SkuEntity
            return skuEntity;
        }, threadPoolExecutor);


        /**
         * 接口二需要拿到接口一的返回值，使用的接口一的线程来串行
         */
        CompletableFuture<Void> categoryCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            // 2.商品详情页接口二：查询sku信息后得到三级分类id，根据三级分类id查询一二三级分类
            ResponseVo<List<CategoryEntity>> listResponseVo = this.gmallPmsClient.queryCategoriesByCid3(skuEntity.getCategoryId());
            List<CategoryEntity> categoryEntityList = listResponseVo.getData();
            // 为ItemVo的属性List<CategoryEntity> categories赋值
            itemVo.setCategories(categoryEntityList);
        }, threadPoolExecutor);


        /**
         * 接口三需要拿到接口一的返回值，使用的接口一的线程来串行
         */
        CompletableFuture<Void> brandCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            // 3.商品详情页接口三：查询sku信息后得到品牌id，根据品牌id查询品牌
            ResponseVo<BrandEntity> brandEntityResponseVo = this.gmallPmsClient.queryBrandById(skuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            if (brandEntity != null) {
                // 为ItemVo的品牌属性赋值
                itemVo.setBrandId(brandEntity.getId());
                itemVo.setBrandName(brandEntity.getName());
            }
        }, threadPoolExecutor);


        /**
         * 接口四需要拿到接口一的返回值，使用的接口一的线程来串行
         */
        CompletableFuture<Void> spuCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            // 4.商品详情页接口四：查询sku信息后得到spuId，根据spuId查询spu信息
            ResponseVo<SpuEntity> spuEntityResponseVo = this.gmallPmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            if (spuEntity != null) {
                // 为ItemVo的spu属性赋值
                itemVo.setSpuId(spuEntity.getId());
                itemVo.setSpuName(spuEntity.getName());
            }
        }, threadPoolExecutor);

        /**
         * 接口五不依赖于其它任务，采取从根上开始串行
         * 也就是从新创建一个任务
         */
        CompletableFuture<Void> skuImagesCompletableFuture = CompletableFuture.runAsync(() -> {

            // 5.商品详情页接口五：根据skuId，查询sku所有图片
            ResponseVo<List<SkuImagesEntity>> skuImagesResponseVo = this.gmallPmsClient.querySkuImagesBySkuId(skuId);
            List<SkuImagesEntity> skuImagesEntityList = skuImagesResponseVo.getData();
            // 为ItemVo的List<SkuImagesEntity> images属性赋值
            itemVo.setImages(skuImagesEntityList);
        }, threadPoolExecutor);


        /**
         * 接口六不依赖于其它任务，采取从根上开始串行
         * 也就是从新创建一个任务
         */
        CompletableFuture<Void> salesCompletableFuture = CompletableFuture.runAsync(() -> {

            // 6.商品详情页接口六：根据skuId，查询sku的所有营销信息，在sms微服务中
            ResponseVo<List<ItemSaleVo>> itemSaleResponseVo = this.gmallSmsClient.querySalesBySkuId(skuId);
            List<ItemSaleVo> itemSaleVoList = itemSaleResponseVo.getData();
            // 为ItemVo的List<ItemSaleVo> sales属性赋值
            itemVo.setSales(itemSaleVoList);
        }, threadPoolExecutor);


        /**
         * 接口七不依赖于其它任务，采取从根上开始串行
         * 也就是从新创建一个任务
         */
        CompletableFuture<Void> storeCompletableFuture = CompletableFuture.runAsync(() -> {
            // 7.商品详情页接口七：根据skuId，查询sku的库存信息，在wms微服务中
            ResponseVo<List<WareSkuEntity>> wareSkuEntityResponseVo = this.gmallWmsClient.queryWareSkuBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntityList = wareSkuEntityResponseVo.getData();
        /*
        库存赋值:
          当数据库表wms_ware_sku中的 stock(库存数) 与 stock_locked(库存) 相减大于0时，
          说明真的有没有被锁定的库存，这样才不会出现超卖现象
          使用stream表达式的方法anyMatch()
          【anyMatch(Predicate p) 传入一个断言型函数，对流中所有的元素进行判断
          只要有一个满足条件就返回true，都不满足返回false】
         */
            if (!CollectionUtils.isEmpty(wareSkuEntityList)) {
                boolean anyMatch = wareSkuEntityList.stream().anyMatch(
                        wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getSales() > 0);
                // 为ItemVo的store属性赋值
                itemVo.setStore(anyMatch);
            }
        }, threadPoolExecutor);


        /**
         * 接口八需要拿到接口一的返回值，使用的接口一的线程来串行
         */
        CompletableFuture<Void> saleAttrsCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            // 8.商品详情页接口八：查询sku信息后得到spuId，根据spuId查询spu下的所有销售属性
            ResponseVo<List<SaleAttrValueVo>> saleAttrValueResponseVo =
                    this.gmallPmsClient.querySkuAttrValuesBySpuId(skuEntity.getSpuId());
            List<SaleAttrValueVo> saleAttrValueVoList = saleAttrValueResponseVo.getData();
            // 为ItemVo的List<SaleAttrValueVo> saleAttrs属性赋值
            itemVo.setSaleAttrs(saleAttrValueVoList);
        }, threadPoolExecutor);


        /**
         * 接口九不依赖于其它任务，采取从根上开始串行
         * 也就是从新创建一个任务
         */
        CompletableFuture<Void> saleAttrCompletableFuture = CompletableFuture.runAsync(() -> {
            // 9.商品详情页接口九：根据skuId，查询当前sku的销售属性
            ResponseVo<List<SkuAttrValueEntity>> skuAttrValueEntityResponseVo =
                    this.gmallPmsClient.querySkuAttrValuesBySkuId(skuId);
            List<SkuAttrValueEntity> skuAttrValueEntityList = skuAttrValueEntityResponseVo.getData();
            if (!CollectionUtils.isEmpty(skuAttrValueEntityList)) {
                /*
                前端需要的效果 {3:'黑色',4:'8G',5:'128G'}，将List<SkuAttrValueEntity>转化为map
                map的key，是attrId
                map的value，是attrValue
                 */
                // 为ItemVo的Map<Long, String> saleAttr属性赋值
                itemVo.setSaleAttr(skuAttrValueEntityList.stream().collect(
                        Collectors.toMap(SkuAttrValueEntity::getAttrId, SkuAttrValueEntity::getAttrValue)
                ));
            }
        }, threadPoolExecutor);


        /**
         * 接口十需要拿到接口一的返回值，使用的接口一的线程来串行
         */
        CompletableFuture<Void> skusJsonCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {

            // 10.商品详情页接口十：根据sku中的spuId查询spu下所有sku，以及所有sku的销售属性组合与skuId映射关系
            ResponseVo<Map<String, Object>> mapResponseVo = this.gmallPmsClient.querySaleAttrsMappingSkuIdBySpuId(skuEntity.getSpuId());
            Map<String, Object> mapData = mapResponseVo.getData();
            // 为ItemVo的skuJsons属性赋值
            itemVo.setSkuJsons(mapData);
        }, threadPoolExecutor);

        /**
         * 接口十一需要拿到接口一的返回值，使用的接口一的线程来串行
         */
        CompletableFuture<Void> spuImagesCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            // 11.商品详情页接口十一：查询sku信息后得到spuId，根据sku中spuId查询spu的描述信息
            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.gmallPmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
            // 对spuDescEntity进行判空
            if (spuDescEntity != null) {
                // 获取spuDescEntity对象中的描述信息
                String decript = spuDescEntity.getDecript();
                // 对decript进行判空，转换需要判空
                if (StringUtils.isNotBlank(decript)) {
                    // 将描述信息转换为以逗号分隔的字符串数组
                    String[] images = StringUtils.split(decript, ",");
                    // 为ItemVo的List<String> spuImages属性赋值，需要将images放入集合再赋值
                    itemVo.setSpuImages(Arrays.asList(images));
                }
            }
        }, threadPoolExecutor);

        /**
         * 接口十二需要拿到接口一的返回值，使用的接口一的线程来串行
         */
        CompletableFuture<Void> groupCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            // 12.商品详情页接口十二：根据分类id、spuId及skuId，查询分组及组下的规格参数值
            ResponseVo<List<ItemGroupVo>> ItemGroupVoResponseVo =
                    this.gmallPmsClient.queryGroupWithAttrValuesByCidAndSpuIdAndSkuId(skuEntity.getCategoryId(), skuEntity.getSpuId(), skuId);
            List<ItemGroupVo> itemGroupVoList = ItemGroupVoResponseVo.getData();
            // 为ItemVo的List<ItemGroupVo> groups属性赋值
            itemVo.setGroups(itemGroupVoList);
        }, threadPoolExecutor);

        // 组合方法并阻塞
        CompletableFuture.allOf(
                skuCompletableFuture, categoryCompletableFuture, brandCompletableFuture,
                spuCompletableFuture, skuImagesCompletableFuture, salesCompletableFuture,
                storeCompletableFuture, saleAttrsCompletableFuture, saleAttrCompletableFuture,
                skusJsonCompletableFuture, spuImagesCompletableFuture, groupCompletableFuture)
                .exceptionally(t -> {
                    log.error("异步任务出现了异常：{}", t.getMessage());
                    return null;
                }).join();

        // 将封装了商品详情信息的ItemVo返回
        return itemVo;
    }

    /**
     * 生成静态页面
     *
     * @param itemVo
     */
    public void createHtml(ItemVo itemVo) {
        /*
        4.为了不影响第一个用户访问的时候，等到生成静态页面才能访问，静态页面采取异步执行
        这样第一个用户可以直接访问动态页面，第二个用户开始就开始访问静态页面
        */
        threadPoolExecutor.execute(() -> {
            /*
            3.
            初始化文件流，输出静态页面到硬盘的某个目录下。
            注意需要提前创建该html目录
            以skuId.html文件名来进行保存
            将文件流写道try的（）中，会自动释放资源，就不用在finally里面进行释放资源了
            这时jdk1.8的新写法
             */
            try (PrintWriter printWriter = new PrintWriter("D:\\Developer_tools\\repos\\IdeaPros\\gmall\\html\\" + + itemVo.getSkuId() + ".html")) {

                // 2.初始化上下文对象
                Context context = new Context();
                // 2.1.页面静态化所需要的数据模型
                context.setVariable("itemVo", itemVo);
                /*
                1.调用模板引擎类的方法process(String template, IContext context, Writer writer)
                参数一：模板名称，要跟item.html名称一致
                参数二：thymeleaf的上下文对象，里面包含模型数据
                参数三：输出静态页面到目的地的流
                 */
                this.templateEngine.process("item", context, printWriter);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
    }
}
