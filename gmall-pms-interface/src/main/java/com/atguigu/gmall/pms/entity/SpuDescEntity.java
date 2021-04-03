package com.atguigu.gmall.pms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

import lombok.Data;

/**
 * spu信息介绍
 *
 * @author Austin
 * @email 985474500@qq.com
 * @date 2021-03-08 23:37:04
 */
@Data
@TableName("pms_spu_desc")
public class SpuDescEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 商品id
     * 因为在配置文件中，指定了id自增，要将其在此处失效
     * 指定为用户填入id：IdType.INPUT
     */
    @TableId(type = IdType.INPUT)
    private Long spuId;
    /**
     * 商品介绍
     */
    private String decript;

}
