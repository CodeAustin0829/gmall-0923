package com.atguigu.gmall.pms.entity.vo;


import com.atguigu.gmall.pms.entity.SpuEntity;
import lombok.Data;

import java.util.List;

/**
 * @Description 用来封装提交后的spu数据
 * 通过分析返回的json数据
 * 除了正常在 SpuEntity 类的属性外，
 * 还多出来了三个属性，来封装数据库查询到的相应字段
 * spuImages、baseAttrs、skus
 * @Author Austin
 * @Date 2021/3/10
 */
@Data
public class SpuVO extends SpuEntity {

    //除了继承SpuEntity，得到四个属性外：name、brandId、categoryId、publishStatus，还需要添加三个属性

    //（1）封装图片信息的属性
    //private List<?> spuImages;
    //图片信息都是字符串，泛型指定为字符串
    private List<String> spuImages;

    //（2）封装基本属性信息的属性
    //private List<?> baseAttrs;
    /*
    基本属性信息，数据对应的表是pms_spu_attr_value，对应的实体类是SpuAttrValueEntity
    attrValue的值在实体类中是String，而传过来时参数名是valueSelected，值为集合类型
    扩展SpuAttrValueEntity类，SpuAttrValueVo
    在里面通过set方法，将得到的集合类型valueSelected，转换为字符串，然后赋值给attrValue
    泛型指定为SpuAttrValueVo
     */
    private List<SpuAttrValueVo> baseAttrs;

    //（3）封装sku信息的属性
    //private List<?> skus;
    private List<SkuVo> skus;

}
