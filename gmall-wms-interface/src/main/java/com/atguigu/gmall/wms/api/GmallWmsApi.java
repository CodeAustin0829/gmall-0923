package com.atguigu.gmall.wms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/15
 */
public interface GmallWmsApi {

    /**
     * （3）ES搜索之根据skuId，查询 sku 的库存信息
     * 同时也是，商品详情页接口七：根据skuId，查询sku的库存信息，在wms微服务中
     */
    @GetMapping("wms/waresku/sku/{skuId}")
    public ResponseVo<List<WareSkuEntity>> queryWareSkuBySkuId(@PathVariable("skuId") Long skuId);


    /**
     * 提交订单时验证库存、锁定库存
     * 参数：用户点击提交订单，提交多个参数，使用Json数据来提交，使用@RequestBody+list集合来接受
     * 返回值：List<SkuLockVo>，参考京东，验证库存、锁定库存后，也要返回有集合，显示库存的状态
     */
    @PostMapping("wms/waresku/check/lock")
    public ResponseVo<List<SkuLockVo>> checkAndLock(@RequestBody List<SkuLockVo> lockVos);
}
