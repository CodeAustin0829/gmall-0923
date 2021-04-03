package com.atguigu.gmall.pms.service.impl;


import com.atguigu.gmall.pms.entity.SpuDescEntity;
import com.atguigu.gmall.pms.entity.vo.SpuVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuDescMapper;

import com.atguigu.gmall.pms.service.SpuDescService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("spuDescService")
public class SpuDescServiceImpl extends ServiceImpl<SpuDescMapper, SpuDescEntity> implements SpuDescService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuDescEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuDescEntity>()
        );

        return new PageResultVo(page);
    }

    //    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Transactional
    /**
     * 注解 @Transactional 只能应用到 public 方法才有效
     * 原因跟 AOP 相关
     * ----------------------------------------------
     * 使用 @Transactional 的三个条件
     * (1)有接口
     * (2)接口有抽象方法
     * 【所以此处，还需要在当前类所继承的接口SpuService上，
     * 加上当前方法的抽象方法】
     * (3)类继承接口后，实现接口的抽象方法必须是public的
     * 因为如果类的方法是私有的private，接口的抽象方法就不能访问到
     * 【 @Transactional 都一般都放置在类的方法上】
     */
    public void saveSpuDesc(SpuVO spu, Long spuId) {
        // 1.3. 保存到pms_spu_desc
        // 1.3.1. 图片信息保存在SpuVO的属性private List<String> spuImages;
        List<String> spuImages = spu.getSpuImages();
        // 1.3.2. 对获取到的图片集合进行判断，如果不为空，才执行保存操作
        if (!CollectionUtils.isEmpty(spuImages)) {
            // 1.3.3. pms_spu_desc对应的实体类是SpuDescEntity
            SpuDescEntity spuDescEntity = new SpuDescEntity();
            //注意：spu_info_desc表的主键是spu_id,需要在实体类中配置该主键不是自增主键
            spuDescEntity.setSpuId(spuId);
            // 把商品的图片描述，保存到spu详情中，图片地址以逗号进行分割
            spuDescEntity.setDecript(StringUtils.join(spuImages, ","));
            // 1.3.4. 将spuDescEntity保存到对应的数据库表中
            this.save(spuDescEntity);
        }
    }

}