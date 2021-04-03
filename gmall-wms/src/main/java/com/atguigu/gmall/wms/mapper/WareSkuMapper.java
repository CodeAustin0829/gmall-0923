package com.atguigu.gmall.wms.mapper;

import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 *
 * @author Austin
 * @email 985474500@qq.com
 * @date 2021-03-09 00:43:26
 */
@Mapper
public interface WareSkuMapper extends BaseMapper<WareSkuEntity> {

    /**
     * 验库存
     */
    public List<WareSkuEntity> check(@Param("skuId") Long skuId, @Param("count") Integer count);

    /**
     * 锁库存
     * 返回值是指影响条数
     */
    public Integer lock(@Param("id") Long id, @Param("count") Integer count);

    /**
     * 解库存
     *
     * @param wareSkuId
     * @param count
     * @return
     */
    public Integer unlock(Long wareSkuId, Integer count);
}
