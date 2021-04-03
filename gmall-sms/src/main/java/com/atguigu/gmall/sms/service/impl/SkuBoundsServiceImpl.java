package com.atguigu.gmall.sms.service.impl;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.gmall.sms.mapper.SkuBoundsMapper;
import com.atguigu.gmall.sms.mapper.SkuFullReductionMapper;
import com.atguigu.gmall.sms.mapper.SkuLadderMapper;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Service("skuBoundsService")
public class SkuBoundsServiceImpl extends ServiceImpl<SkuBoundsMapper, SkuBoundsEntity> implements SkuBoundsService {

    @Autowired
    private SkuFullReductionMapper skuFullReductionMapper;
    @Autowired
    private SkuLadderMapper skuLadderMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuBoundsEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuBoundsEntity>()
        );

        return new PageResultVo(page);
    }

    /**
     * 新增sku的营销信息
     */
    @Override
    @Transactional
    public void saveSkuSale(SkuSaleVo skuSaleVo) {
        //保存营销信息
        // 1.1. 保存到sms_sku_bounds
        // sms_sku_bounds 对应的实体类是 SkuBoundsEntity
        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        // 将skuSaleVo对象中的属性，对拷到skuBoundsEntity对象中的属性
        BeanUtils.copyProperties(skuSaleVo, skuBoundsEntity);
        // 属性work无法对拷，因为数据库保存的是整数0-15[tinyint类型]，页面绑定是0000-1111[List<Integer>类型]，所以需要转换一下
        List<Integer> work = skuSaleVo.getWork();
        // 当获取到的work不为空，才进行转换
        if (!CollectionUtils.isEmpty(work)) {
            //也就是将前端页面的二进制数字，转换为十进制数据，但是int类型，在数据库进行保存的时候，高位为0，会自动忽略掉
            //例如前端是0001，保存到数据库的时候，就是1了，所以需要反向保存一下
            skuBoundsEntity.setWork(work.get(0) * 8 + work.get(1) * 4 + work.get(2) * 2 + work.get(3));
        }
        // 将skuBoundsEntity保存到数据库表中
        this.save(skuBoundsEntity);


        // 1.2. 保存到sms_sku_full_reduction
        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(skuSaleVo, skuFullReductionEntity);
        //add_other字段跟前端返回来的数据名称[fullAddOther]不一致，所以还需要手动添加一下
        skuFullReductionEntity.setAddOther(skuSaleVo.getFullAddOther());
        this.skuFullReductionMapper.insert(skuFullReductionEntity);

        // 1.3. 保存到sms_sku_ladder
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        BeanUtils.copyProperties(skuSaleVo, skuLadderEntity);
        skuLadderEntity.setAddOther(skuSaleVo.getLadderAddOther());
        this.skuLadderMapper.insert(skuLadderEntity);
    }

    /**
     * 商品详情页接口六：根据skuId，查询sku的所有营销信息
     */
    @Override
    public List<ItemSaleVo> querySalesBySkuId(Long skuId) {
        //（4）第四步：创建一个List集合，用来封装查询的三张表数据
        List<ItemSaleVo> itemSaleVoList = new ArrayList<>();

        //（1）第一步：sms_sku_bound表，查询积分活动
        SkuBoundsEntity skuBoundsEntity =
                this.getOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id", skuId));
        if (skuBoundsEntity != null) {
            // 创建ItemSaleVo对象，用来设置查询到的积分数据
            ItemSaleVo bounds = new ItemSaleVo();
            bounds.setDesc("积分");
            bounds.setType("送" + skuBoundsEntity.getBuyBounds()
                    + "购物积分，送" + skuBoundsEntity.getGrowBounds() + "成长积分");
            // 将ItemSaleVo封装到itemSaleVoList集合中
            itemSaleVoList.add(bounds);
        }

        //（2）第二步：sms_sku_full_reduction表，查询满减活动
        SkuFullReductionEntity skuFullReductionEntity =
                this.skuFullReductionMapper.selectOne(new QueryWrapper<SkuFullReductionEntity>().eq("sku_id", skuId));
        if (skuFullReductionEntity != null) {
            // 创建ItemSaleVo对象，用来设置查询到的满减数据，提取到全局，设置相同属性时，后面的值会覆盖掉前面的值
            ItemSaleVo reduction = new ItemSaleVo();
            reduction.setDesc("满减");
            reduction.setType("满" + skuFullReductionEntity.getFullPrice()
                    + "减" + skuFullReductionEntity.getReducePrice());
            // 将ItemSaleVo封装到itemSaleVoList集合中
            itemSaleVoList.add(reduction);
        }

        //（3）第三步：sms_sku_ladder表，查询打折活动
        SkuLadderEntity skuLadderEntity =
                this.skuLadderMapper.selectOne(new QueryWrapper<SkuLadderEntity>().eq("sku_id", skuId));
        if (skuLadderEntity != null) {
            // 创建ItemSaleVo对象，用来设置查询到的积分数据
            ItemSaleVo ladder = new ItemSaleVo();
            ladder.setDesc("打折");
            // 打折是以%来算，所以要除以10，也就是零点几折，调方法divide(new BigDecimal(10))即可
            ladder.setType("满" + skuLadderEntity.getFullCount() + "几件，打" +
                    skuLadderEntity.getDiscount().divide(new BigDecimal(10)) + "折");
            // 将ItemSaleVo封装到itemSaleVoList集合中
            itemSaleVoList.add(ladder);
        }

        //（5）第五步：将集合List<ItemSaleVo>返回
        return itemSaleVoList;
    }

}