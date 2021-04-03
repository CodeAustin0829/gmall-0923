package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @Description 定义一个切面类，增强 @GmallCache 注解
 * @Author Austin
 * @Date 2021/3/23
 */
// 1.声明当前类是一个切面类
@Aspect
// 2.将当前切面类，放入到Spring IOC容器中
@Component
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RBloomFilter rBloomFilter;

    // 3.利用AOP的环绕通知，进行方法增强，@annotation(GmallCache)表示拦截注解
    @Around("@annotation(GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {

        /*
        4.1.提供get(缓存Key)方法需要的参数：缓存Key的前缀
        先获取切点方法的签名
        再获取切点方法的对象
        再获取切点方法上特定注解的对象
        再获取特定注解中的前缀
         */
        MethodSignature signature = (MethodSignature) joinPoint.getSignature(); //切点方法的签名
        Method method = signature.getMethod(); //切点方法的对象
        GmallCache gmallCache = method.getAnnotation(GmallCache.class); //切点方法上特定注解的对象
        String prefix = gmallCache.prefix(); //特定注解中的前缀
        /*
        4.2.提供get(缓存Key)方法需要的参数：缓存Key的后缀
        也就是IndexService类的queryLvl2CategoriesWithSub方法的参数pid【命名规范："项目名:功能名:key"】
        故需要获取切点方法的参数
        因为得到的参数是个数组，而数组的toString()方法，拿到也是地址【打印结果是[类型@哈希值]】
        所以将数组转换为集合，然后再调用toString()，转为字符串
         */
        Object[] args = joinPoint.getArgs();
        String param = Arrays.asList(args).toString();
        // 4.5. 获取切点方法的返回值，方便Json反序列化
        Class<?> returnType = method.getReturnType();

        // 10.通过布隆过滤器判断数据是否存在[通过缓存key]，防止缓存雪崩
        if (!this.rBloomFilter.contains(prefix + param)) {
            return null;
        }

        // 4.增强被拦截的注解所在方法，拦截前代码块：查询缓存中有没有数据，根据缓存Key来获取查询到的Json数据
        String json = this.stringRedisTemplate.opsForValue().get(prefix + param);
        // 4.3.判断从缓存查询到Json数据是否为空
        if (StringUtils.isNotBlank(json)) {
            /*
             4.4.如果Json数据不为空，反序列化成所需要的类型，然后返回
             此处返回的就是被增强的方法queryLvl2CategoriesWithSub方法的返回值List<CategoryEntity>
             返回值在第4.5.步进行获取
             */
            return JSON.parseObject(json, returnType);
        }

        // 5.1.从特定注解中获取lockKey
        String lock = gmallCache.lock();
        // 5.缓存中没有数据，添加分布式锁，防止缓存击穿，也需要lockKey
        RLock rLock = this.redissonClient.getLock(lock + param);
        // 5.2.获取锁
        rLock.lock();

        try {
            // 6.再次判断缓存中有没有数据，有直接返回(加锁的过程中，别的请求可能已经把数据放入缓存)，没有再让去查数据库
            String json2 = this.stringRedisTemplate.opsForValue().get(prefix + param);
            if (StringUtils.isNotBlank(json2)) {
                return JSON.parseObject(json2, returnType);
            }

            /*
            7.执行目标方法[切面方法]，也就是执行IndexService类的queryLvl2CategoriesWithSub方法
            joinPoint.getArgs()，获取参数，就相当于pid
             */
            Object result = joinPoint.proceed(joinPoint.getArgs());

            // 8.1.设置缓存的过期时间，从注解对象中获取
            int timeout = gmallCache.timeout();
            // 8.2.为防止缓存雪崩，给缓存过期时间设置随机值，从注解对象中获取
            int random = gmallCache.random();
            // 8.拦截后代码块：将从数据库查询到的数据，放入缓存，然后释放分布锁
            this.stringRedisTemplate.opsForValue().set(prefix + param, JSON.toJSONString(result), timeout + new Random().nextInt(random), TimeUnit.MINUTES);

            // 9.返回查询到的数据
            return result;
        } finally {
            /*
            6.1.第二次查询缓存，要放进try里面，然后在finally里面释放锁
            因为有可能第二次查询，查询到数据，就直接返回了
            此时锁没有释放，会造成死锁的现象
            所以finally里面，不断有没有查询到数据，最终都会释放锁
             */
            rLock.unlock();
        }
    }
}
