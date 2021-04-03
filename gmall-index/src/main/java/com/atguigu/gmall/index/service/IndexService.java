package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.aspect.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsFeign;
import com.atguigu.gmall.index.utils.DistributedLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Description
 * @Author Austin
 * @Date 2021/3/20
 */
@Service
public class IndexService {

    // 远程调用gmall-pms-interface
    @Autowired
    private GmallPmsFeign gmallPmsFeign;

    // 注入redis缓存模板
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 定义保存到redis中的key，"项目名:功能名:key"，会以树状结构保存到redis中
    private static final String KEY_PREFIX = "index:category:";
    private static final String LOCK_PREFIX = "index:cates:";

    //注入可重入锁的工具类
    @Autowired
    private DistributedLock distributedLock;

    //注入redisson客户端
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 跳转到首页，无需在域名后面加路径
     *
     * @return
     */
    public List<CategoryEntity> queryLvl1Categories() {
        // 远程调用pms的方法，传parentId=0过去，查询的就是一级分类
        ResponseVo<List<CategoryEntity>> listResponseVo = this.gmallPmsFeign.queryCategoryByParentId(0L);
        // 从ResponseVo的data属性中，获取List<CategoryEntity>
        List<CategoryEntity> categoryEntityList = listResponseVo.getData();
        return categoryEntityList;
    }

    /**
     * 首页查询二级分类及三级分类
     *
     * @param pid
     * @return
     */
    public List<CategoryEntity> queryLvl2CategoriesWithSub1(Long pid) {
        // 先查询redis缓存[以pid来进行查询]，如果缓存有数据，直接返回查询，如果缓存没有数据，再前往远程调用查询数据库，返回的Json字符串
        String JsonCacheCategories = this.stringRedisTemplate.opsForValue().get(KEY_PREFIX + pid);
        // 判断查询缓存的数据是否为空
        if (StringUtils.isNotBlank(JsonCacheCategories)) {
            // 如果缓存中有数据，将Json字符串反序列化成集合，返回给前端
            // 第一个参数：Json字符串，第二个参数：集合的泛型
            List<CategoryEntity> categoryEntityList = JSON.parseArray(JsonCacheCategories, CategoryEntity.class);
            return categoryEntityList;
        }

        /**
         * 为了防止缓存击穿，添加分布式锁
         * 在大量请求到达redis，而redis没有缓存数据时，
         * 请求会全部直达数据库，为了解决，在redis代码之后，访问数据库之前添加上分布式锁
         */
        RLock fairLock = this.redissonClient.getFairLock(LOCK_PREFIX + pid);
        fairLock.lock();//获取锁


        try {
            /**
             * 在获取分布式锁的过程中，可能会有其它的请求已经将数据放入到缓存
             * 此时还应该再查一遍缓存
             * 确认缓存中是否存在数据
             */
            String JsonCacheCategories2 = this.stringRedisTemplate.opsForValue().get(KEY_PREFIX + pid);
            if (StringUtils.isNotBlank(JsonCacheCategories2)) {
                List<CategoryEntity> categoryEntityList = JSON.parseArray(JsonCacheCategories2, CategoryEntity.class);
                return categoryEntityList;
            }
            // 如果缓存中没有数据，远程调用查询数据库
            ResponseVo<List<CategoryEntity>> listResponseVo = this.gmallPmsFeign.queryLevel2CategoryWithSubsByPid(pid);
            List<CategoryEntity> categoryEntityList = listResponseVo.getData();
            if (CollectionUtils.isEmpty(categoryEntityList)) {
                /**
                 * 缓存穿透：为了防止缓存穿透，数据即使为空，也进行缓存，但是缓存时间较短，一般不超过5分钟
                 */
                this.stringRedisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntityList), 5, TimeUnit.MINUTES);
            } else {
                // 查询到数据库后，先往redis中缓存一份，下次再来查时，直接走缓存，将集合转为Json字符串来进行缓存
                // 第三个参数：密钥过期超时时间，第四个参数：单位
                /**
                 * 缓存雪崩：为了防止缓存雪崩，给缓存时间添加随机值
                 * new Random().nextInt(30) 表示添加30以内的随机值
                 */
                this.stringRedisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntityList), 180 + new Random().nextInt(30), TimeUnit.DAYS);
            }
            return categoryEntityList;
        } finally {
            //解锁
            fairLock.unlock();
        }
    }

    /**
     * 首页查询二级分类及三级分类，使用自定义的缓存注解 @GmallCache
     *
     * @param pid
     * @return
     */
    @GmallCache(prefix = "index:cates:", timeout = 14400, random = 3600, lock = "lock")
    public List<CategoryEntity> queryLvl2CategoriesWithSub(Long pid) {
        // 远程调用查询数据库
        ResponseVo<List<CategoryEntity>> listResponseVo = this.gmallPmsFeign.queryLevel2CategoryWithSubsByPid(pid);
        List<CategoryEntity> categoryEntityList = listResponseVo.getData();
        return categoryEntityList;
    }

    /**
     * 锁的压力测试
     */
    public void testlock1() {
        /**
         * 基于redis实现分布式锁
         * （1）第一步： 从redis中获取锁，setnx
         * key需要是唯一不重复的
         */
        // 为了保证能在服务器宕机的情况下，释放锁，为获取到的锁设定过期时间
        // 为了保证原子性，在redis中的同一条指令中，设置过期时间
        // 为了防止误删除，为每个锁设置一个唯一的值[UUID]
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid, 30, TimeUnit.SECONDS);
        /**
         * (2)第二步：
         * 如果获取锁失败，则进行重试
         */
        if (!lock) {
            try {
                Thread.sleep(80); //每隔80回调一次，再次尝试获取锁
                testlock1(); //递归调用，只要重复调用方法，就可以重复执行第一行代码，不断获取锁
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            /**
             * (3)第三步：如果获取锁成功，执行业务代码
             */
            // 先再Linux中往redis设置set num 0，然后查询redis中的num
            String numString = this.stringRedisTemplate.opsForValue().get("num");
            // 判断查询到的数据，是否为空，如果为空，则直接return结束方法
            if (StringUtils.isBlank(numString)) {
                return;
            }
            // 如果数据不为空，先得到的字符串numString转换为int类型，然后加加
            int numInt = Integer.parseInt(numString);
            // 每次查询，让redis中的num进行++，++完后再换回字符串
            this.stringRedisTemplate.opsForValue().set("num", String.valueOf(++numInt));
            // 使用ab进行压力测试

            /**
             * (4)第四步：释放锁，通过key进行del即可
             * 释放锁之前，先根据锁的Value，跟uuid对比，来判断是不是自己的锁
             */
            /*if (StringUtils.equals(uuid, this.stringRedisTemplate.opsForValue().get("lock"))) {
                this.stringRedisTemplate.delete("lock");
            }*/
            //使用lua脚本保证判断和删除之间的原子性
            /*
            声明lua脚本
            redis.call表示执行redis的指令
            'get'执行get指令，通过key，获取值，KEY[1]表示动态传参
            ARGV[1]表示动态传参，传进来的是值
            then 表示成功后的执行，尔后删除锁返回
            else表示失败后的执行，返回0表示失败，end表示判断语句结束
             */
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            /*
            执行lua脚本，使用execute
            第一个参数：new一个脚本对象，new接口 RedisScript的实现类，然后将上面的脚本字符串设置进去
                       同时要设置lua脚本返回值，if判断，是Boolean
            第二个参数：是指传值给KEYS[1]，是个集合
            第三个参数：是指传值给ARGV[1]，是个不定参数
             */
            this.stringRedisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList("lock"), uuid);
        }
    }

    /**
     * 锁的可重入测试
     */
    public void testlock2() {
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.distributedLock.tryLock("lock", uuid, 30L);
        if (lock) {
            //如果获取锁成功，执行业务代码
            String numString = this.stringRedisTemplate.opsForValue().get("num");
            // 判断查询到的数据，是否为空，如果为空，则直接return结束方法
            if (StringUtils.isBlank(numString)) {
                return;
            }
            // 如果数据不为空，先得到的字符串numString转换为int类型，然后加加
            int numInt = Integer.parseInt(numString);
            // 每次查询，让redis中的num进行++，++完后再换回字符串
            this.stringRedisTemplate.opsForValue().set("num", String.valueOf(++numInt));

            try {
                Thread.sleep(60);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 测试可重入性，也就是A调用B方法，看锁是否可重入
//            this.testSubLock(uuid);


            // 释放锁
            this.distributedLock.unlock("lock", uuid);
        }
    }

    /**
     * 测试可重入性
     */
    private void testSubLock(String uuid) {
        // 加锁
        Boolean lock = this.distributedLock.tryLock("lock", uuid, 30L);

        if (lock) {
            System.out.println("测试分布式可重入锁。。。");

            this.distributedLock.unlock("lock", uuid);
        }
    }

    /**
     * 使用redisson框架获取锁、释放锁
     */
    public void testlock() {
        // 使用redisson客户端获取锁
        RLock lock = this.redissonClient.getLock("lock");
        lock.lock(); // 加锁

        /**
         * debug会阻塞线程，导致自动续期无法执行
         */
        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //如果获取锁成功，执行业务代码
        String numString = this.stringRedisTemplate.opsForValue().get("num");
        // 判断查询到的数据，是否为空，如果为空，则直接return结束方法
        if (StringUtils.isBlank(numString)) {
            return;
        }
        // 如果数据不为空，先得到的字符串numString转换为int类型，然后加加
        int numInt = Integer.parseInt(numString);
        // 每次查询，让redis中的num进行++，++完后再换回字符串
        this.stringRedisTemplate.opsForValue().set("num", String.valueOf(++numInt));

        // 释放锁
        lock.unlock();
    }

    /**
     * 读的测试
     */
    public void readLock() {
        //初始化一个读写锁
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        //获取读锁
        RLock rLock = rwLock.readLock();
        //为读锁设置过期时间10秒，这样就不用手动解锁了
        rLock.lock(10, TimeUnit.SECONDS);
        //测试不做操作也可
        System.out.println(" 测试读锁 ");
        //手动解锁
        //rLock.unlock();
    }

    /**
     * 写的测试
     */
    public void writeLock() {
        //初始化一个读写锁
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        //获取读锁
        RLock wLock = rwLock.writeLock();
        //为读锁设置过期时间10秒，这样就不用手动解锁了
        wLock.lock(10, TimeUnit.SECONDS);
        //测试不做操作也可
        System.out.println(" 测试写锁 ");
        //手动解锁
        //wLock.unlock();
    }

    /**
     * 闭锁（CountDownLatch）的测试
     * 等待latch
     */
    public void latch() {
        try {
            //获取闭锁
            RCountDownLatch countdown = this.redissonClient.getCountDownLatch("countdown");
            // 让班长等待6人出门
            countdown.trySetCount(6);
            countdown.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 闭锁（CountDownLatch）的测试
     * 计数
     */
    public void countDown() {
        //获取闭锁
        RCountDownLatch countdown = this.redissonClient.getCountDownLatch("countdown");
        //每次访问，计数一次
        countdown.countDown();
    }
}

