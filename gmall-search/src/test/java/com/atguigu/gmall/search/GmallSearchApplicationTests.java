package com.atguigu.gmall.search;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsFeignClient;
import com.atguigu.gmall.search.feign.GmallWmsFeignClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.search.vo.SearchAttrValueVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {

    /**
     * 自动注入ESRest客户端模板
     * ElasticsearchTemplate，是TransportClient客户端
     * ElasticsearchRestTemplate，是RestHighLevel客户端
     */
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    /**
     * 自动注入 GoodsRepository
     */
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

    /**
     * 创建索引及映射
     */
    @Test
    void contextLoads() {
        //基于ESRest客户端模板，创建索引库
        this.elasticsearchRestTemplate.createIndex(Goods.class);
        //基于ESRest客户端模板，创建映射
        this.elasticsearchRestTemplate.putMapping(Goods.class);
    }

    /**
     * 导入文档数据【行数据】，在这里面进行远程调用pms、wms，查询出数据，保存到ES中
     * -----------------------------------------------------------------------
     * ElasticsearchRestTemplate 没有提供对新建文档的操作
     * 需要继承ElasticsearchRepository接口来实现
     * （1）自定义一个接口 GoodsRepository
     * （2）继承 ElasticsearchRepository<T, ID> 接口
     * 泛型1：即返回结果集，也就是查询的返回对象User
     * 泛型2：ID的类型，也就是Long类型
     * （3）在当前类注入 GoodsRepository，进行文档的CRUD
     */
    @Test
    void testAddAll() {
        /**
         * 使用 do...while 循环语句 进行数据导入
         * -----------------------------------
         * 【知识点回顾】
         * （1）扩展格式：
         * 初始化语句①
         * do {
         *      循环体语句②；
         *      迭代语句③；
         * } while (循环条件语句④)；
         * -----------------------
         * （2）执行流程：
         * 第一步：执行初始化语句①，完成循环变量的初始化；
         * 第二步：执行循环体语句②；
         * 第三步：执行迭代语句③，针对循环变量重新赋值；
         * 第四步：执行循环条件语句④，看循环条件语句的值是true，还是false；
         *        如果是true，根据循环变量的新值，重新从第二步开始再执行一遍；
         *        如果是false，循环语句中止，循环不再执行。
         * --------------------------------------------
         * （3）注意事项：
         * while(循环条件)中循环条件必须是boolean类型
         * do{}while();最后有一个分号
         * do...while结构的循环体语句是至少会执行一次，这个和for和while是不一样的
         */
        // 一、初始化语句
        // 页数，从分页数据的第1页开始进行导入，导入完成，就让pageNum++
        Integer pageNum = 1;
        // 页面数据大小，每页导入100条件数据
        Integer pageSize = 100;

        do {
            /**
             * 1.第一步：分批查询spu
             */
            // 1.2 提供参数：分页查询参数对象PageParamVo
            PageParamVo pageParamVo = new PageParamVo();
            // 1.3 设置查询条件
            pageParamVo.setPageNum(pageNum);
            pageParamVo.setPageSize(pageSize);
            // 1.1 远程调用pms微服务，查询spu
            ResponseVo<List<SpuEntity>> spuResponseVo = this.gmallPmsFeignClient.querySpuPage(pageParamVo);
            // 1.4 从ResponseVo中获取List<SpuEntity>，已封装在其data属性中
            List<SpuEntity> spuEntityList = spuResponseVo.getData();
            // 1.5 List<SpuEntity>也就是当前页对象，size()获取它的长度，赋值给循环体语句pageSize，以进行判断
            int size = spuEntityList.size();


            /**
             * 2.第二步：遍历spu，查询spu下的所有sku
             */
            // 2.1 获取到的集合spuEntityList，有可能为空，判断为空则结束，不为空则进行遍历
            if (CollectionUtils.isEmpty(spuEntityList)) {
                return;
            }
            // 2.2 遍历spuEntityList，得到每一个spuEntity对象
            spuEntityList.forEach(spuEntity -> {
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
                                  当数据库表wms_ware_sku中的 stock(库存数) 与 stock_locked(锁定库存) 相减大于0时，
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
                            }).collect(Collectors.toList())
                    );
                }
            });

            /*
            三、第三步：循环体语句
            每导入1页，如果可以进行下一页，也就是进入循环，让页数++
            然后取出当前页的页面数据大小，赋值给pageSize，来进行判断，看是否==100
             */
            pageNum++;
            //pageSize = null;
            pageSize = size;

        /*
         二、第二步：while (循环条件语句)
         条件是 pageSize == 100
         当页面数据 = 100时，也就是true，说明还有下一页，继续循环导入；
         当页面数据 != 100时，也就是false，说明没有下一页，结束循环导入
         */
        } while (pageSize == 100);

    }

}
