package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.service.WareSkuService;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuMapper, WareSkuEntity> implements WareSkuService {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private WareSkuMapper wareSkuMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String LOCK_PREFIX = "stock:lock:";
    private static final String KEY_PREFIX = "stock:ware:";

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WareSkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageResultVo(page);
    }

    /**
     * 提交订单时验证库存、锁定库存
     * 参数：用户点击提交订单，提交多个参数，使用Json数据来提交，使用@RequestBody+list集合来接受
     * 返回值：List<SkuLockVo>，参考京东，验证库存、锁定库存后，也要返回有集合，显示库存的状态
     */
    @Override
    public List<SkuLockVo> checkAndLock(List<SkuLockVo> lockVos) {
        // 1.第一步：判断传递的参数lockVos是否为空，为空则抛出异常
        if (CollectionUtils.isEmpty(lockVos)) {
            throw new OrderException("您没有选中的商品!");
        }

        // 2.第二步：遍历集合lockVos，对每一个商品验库存并锁库存
        lockVos.forEach(skuLockVo -> {
            // 提取代码成方法
            this.checkLock(skuLockVo);
        });

        // 3.判断是否存在锁定失败的商品，如果存在，所有已经成功锁定的商品要解库存，类似事务的回滚
        // 3.1.从前端传递过来的List<SkuLockVo>中获取锁定状态的属性lock
        // 3.1.1.获取锁定成功的锁定信息集合
        List<SkuLockVo> successLockVO =
                lockVos.stream().filter(lockVo -> lockVo.getLock()).collect(Collectors.toList());
        // 3.1.2.获取锁定失败的锁定信息集合
        List<SkuLockVo> errorLockVO =
                lockVos.stream().filter(lockVo -> !lockVo.getLock()).collect(Collectors.toList());
        // 3.2.判断errorLockVO不为空，说明锁定失败
        if (!CollectionUtils.isEmpty(errorLockVO)) {
            // 3.2.1.锁定失败，则遍历锁定成功的锁定信息集合，进行解库存
            successLockVO.forEach(lockVo -> {
                this.wareSkuMapper.unlock(lockVo.getWareSkuId(), lockVo.getCount());
            });
            // 3.2.2.解库存失败的时候，还需要返回一个失败信息的返回给页面进行显示，已包含在lockVos中
            return lockVos;
        }
        //3.3.
        // 把库存的锁定信息保存到redis中，以方便将来解锁库存
        String orderToken = lockVos.get(0).getOrderToken();
        this.stringRedisTemplate.opsForValue().set(KEY_PREFIX + orderToken, JSON.toJSONString(lockVos));

        return null; // 如果都锁定成功，不需要展示锁定情况
    }

    // 代码提取：对每一个商品验库存并锁库存
    public void checkLock(SkuLockVo skuLockVo) {
        // 为了保证验库存和锁库存的原子性【原子操作是不可分割的，在执行完毕不会被任何其它任务或事件中断】，放在同一方法里面就是为了保证验证和锁定的原子性
        // 分布式锁，防止多人同时锁库存
        RLock lock = this.redissonClient.getFairLock(LOCK_PREFIX + skuLockVo.getSkuId());
        // 获取锁
        lock.lock();
        try {
            // 验库存：本质就是查询库存
            List<WareSkuEntity> wareSkuEntities =
                    this.wareSkuMapper.check(skuLockVo.getSkuId(), skuLockVo.getCount());
            // 对wareSkuEntities进行判断，假设为空，就是没库存
            if (CollectionUtils.isEmpty(wareSkuEntities)) {
                skuLockVo.setLock(false);
                return;
            }
            // 如果不为空，从最近的仓库锁库存，这里我们取第一个仓库
            WareSkuEntity wareSkuEntity = wareSkuEntities.get(0);
            // 当根据商品id和商品数量去数据库查询，返回的是1，说明锁定库存成功了，返回的是0，说明锁定库存失败了
            if (this.wareSkuMapper.lock(wareSkuEntity.getSkuId(), skuLockVo.getCount()) == 1) {
                skuLockVo.setLock(true); //锁定库存成功
                skuLockVo.setWareSkuId(wareSkuEntity.getId());// 记录锁定是哪一条库存
            } else {
                skuLockVo.setLock(false);
            }
        } finally {
            // 释放锁
            lock.unlock();
        }
    }
}