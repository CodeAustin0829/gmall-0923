package com.atguigu.gmall.search.pojo;

import com.atguigu.gmall.search.vo.SearchAttrValueVo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.List;

/**
 * @Description 操作 ElasticSearch 的实体类
 * @Author Austin
 * @Date 2021/3/15
 */

@Data
@AllArgsConstructor //全参构造器
@NoArgsConstructor  //无参构造器
/**
 * `@Document` 作用在类，标记实体类为文档对象，一般有四个属性
 * - indexName：对应索引库名称
 * - type：对应在索引库中的类型【也就是MySQL中的表】
 * - shards：分片数量，默认5
 * - replicas：副本数量，默认1
 */
@Document(indexName = "goods", type = "info", shards = 3, replicas = 2)
public class Goods {

    /**
     * `@Id` 作用在成员变量，标记一个字段作为id主键，会映射到ES中的"_id"
     * ---------------------------------------------------------------------------------
     * `@Field` 作用在成员变量，标记为文档的字段，并指定字段映射属性：
     * - type：字段类型，取值是枚举：FieldType
     * 一般取Java属性中的类型，只有String类型时，才会有Text（分词）、Keyword（不分词）的选择
     * - index：是否索引，布尔类型，默认是true【所以如果要建立索引时，可以省略不写】
     * - store：是否存储，布尔类型，默认是false【一般省略不写】
     * - analyzer：分词器名称：ik_max_word
     */

    /**
     * （1）
     * 商品列表字段\搜索列表字段：
     * 对比京东页面，每条记录就是一个sku，也就是商品列表
     * 每条记录都有自己的唯一标识，也就是skuId；
     * 除了skuId，商品列表还需要四个字段：
     * 标题、副标题、sku的价格、默认图片
     */
    @Id
    private Long skuId;
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;
    @Field(type = FieldType.Keyword, index = false)
    private String subTitle;
    @Field(type = FieldType.Double)
    private Double price;
    @Field(type = FieldType.Keyword, index = false)
    private String defaultImage;


    /**
     * （2）
     * 排序及分页字段：
     * 综合（通过得分来排序，所以不需要字段）、新品（创建时间越短，商品越新）、销量、价格、库存（是否有货）
     */
    @Field(type = FieldType.Long)
    private Long sales = 0l; // 销量
    @Field(type = FieldType.Date)
    private Date createTime; // 新品，日期时间类型
    @Field(type = FieldType.Boolean)
    private boolean store = false; // 是否有货

    /**
     * （3）
     * 聚合字段：
     * 品牌聚合、分类聚合、搜索规格参数聚合（多个）
     */
    //品牌过滤
    @Field(type = FieldType.Long)
    private Long brandId; // 根据品牌Id，聚合出品牌
    @Field(type = FieldType.Keyword)
    private String brandName; // 聚合出品牌后，鼠标移入显示品牌名称，移除显示品牌logo，避免再次查询数据库
    @Field(type = FieldType.Keyword)
    private String logo;
    //分类过滤
    @Field(type = FieldType.Long)
    private Long categoryId; //根据分类Id来聚合
    @Field(type = FieldType.Keyword)
    private String categoryName; //聚合出分类之后，使用分类名称来进行显示
    // 规格参数过滤，有多个，提供给一个List，方便后续扩容字段
    // Nested表示是一个嵌套类型
    @Field(type = FieldType.Nested)
    private List<SearchAttrValueVo> searchAttrs;

    /**
     * 【搜索数据的整体业务分析】
     * 实现目标：
     * gmall-search --远程调用--> gmall-pms 的接口
     * gmall-pms 从数据库中查询出数据
     * gmall-search 得到数据后，保存到 ES 中
     * ----------------------------------------------------------------------------------
     * （一）gmall-search
     * 对比京东页面，分析出有哪些属性，需要导入ES的字段当中，然后根据 ES 所需字段，封装 JavaBean
     * (1)商品列表字段：
     *                skuId、标题、副标题、sku的价格、默认图片
     * (2)排序及分页字段：
     *                 新品（创建时间越短，商品越新）、销量、库存（是否有货）【分页无需字段】
     * (3)聚合字段：
     *            ① 品牌聚合：品牌Id、品牌名称、品牌logo
     *            ② 分类聚合：分类Id、分类名称
     *            ③ 规格参数聚合：规格参数Id、规格参数名称、规格参数对应的值
     * ----------------------------------------------------------------------------------
     * （二）gmall-pms
     * 针对 ES 所需字段，gmall-search 远程调用 gmall-pms 的接口，来查询出数据，那么，gmall-pms
     * 就要提供相对应的查询接口
     * 为了缩小查询次数，从上往下查：pms_spu --> pms_sku --> wms_ware_sku
     * (1)接口一：
     * 分页查询，查询 pms_spu，得到字段：新品【create_time】、spuId、分类Id、品牌Id
     * 在 SpuController
     * (2)接口二：
     * 根据spuId，查询 pms_sku，得到字段：skuId、sku标题、sku副标题、sku价格、sku默认图片
     * 在 SkuController
     * (3)接口三：
     * 根据skuId，查询 wms_ware_sku，得到字段：销量【sales】、库存【stock】
     * 在 WareSkuController
     * (4)接口四：
     * 根据品牌Id，查询 pms_brand，查询出品牌
     * 在 BrandController
     * (5)接口五：
     * 根据分类Id，查询 pms_category，查询出分类
     * 在 CategoryController
     * ----------------------------------------------------------------------------------
     * (6)接口六：
     * 根据
     * 分类Id、
     * search_type=1【是否需要检索[0-不需要，1-需要]】、
     * skuId
     * 查询检索类型的销售属性及值
     * 查询的是 pms_attr 和 pms_sku_attr_value
     *
     * 【联表查询SQL语句】
     * -- 查询A、B两表的交集，使用内连接INNER JOIN
     * SELECT t1.*
     * FROM pms_sku_attr_value t1
     * INNER JOIN pms_attr t2
     * ON t1.attr_id = t2.id
     * WHERE
     * t2.category_id = 225
     * AND
     * t2.search_type = 1
     * AND
     * t1.sku_id = 3
     * -- 但是一般不建议使用联表查询，而是使用分步查询
     * -- （1）先查询出：pms_attr表中search_type = 1 和 category_id = 225的数据
     * SELECT *
     * FROM pms_attr
     * WHERE
     * category_id = 225
     * AND
     * search_type = 1
     * -- （2）再查询出：再根据skuId、attrId，查询pms_sku_attr_value表中，符合t1.attr_id = t2.id的数据【但是这只是属于联表查询的ON】，这里只要指出范围即可
     * SELECT *
     * FROM pms_sku_attr_value
     * WHERE
     * sku_id = 3
     * AND
     * attr_id in (4,5,6,8,9)
     * -- 这样的分布查询，跟上面的联表查询结果是一样的
     * ----------------------------------------------------------------------------------
     * (7)接口七：
     * 根据
     * 分类Id、
     * search_type=1【是否需要检索[0-不需要，1-需要]】、
     * spuId
     * 查询检索类型的基本属性及值
     * SQL语句跟(6)差不多
     * ----------------------------------------------------------------------------------
     *
     */
}
