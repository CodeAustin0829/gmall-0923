package com.atguigu.gmall.index.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @Description 可重入锁的工具类：获取锁、释放锁；自动续期定时任务
 * @Author Austin
 * @Date 2021/3/22
 */
@Component // 将当前类注入到Spring IOC容器当中
public class DistributedLock {

    // 注入redis缓存模板
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 看门狗定时任务，提升变量作用域
    private Timer timer;

    /**
     * 获取锁
     *
     * @return Boolean，判断获取锁是否成功
     */
    public Boolean tryLock(String lockName, String uuid, Long expire) {
        /**
         * 【redis知识点回顾】
         * Redis中存储数据是通过key-value存储的：key都是String类型，value有五种数据类型
         * ------------------------------------------------------------------------
         * redis常用指令：针对Key的操作
         * （1）exists key：判断某个key是否存在
         * （2）del key：删除指定的key数据
         * （3）EXPIRE key seconds：设置key的生存时间（单位：秒）key在多少秒后会自动删除
         * ----------------------------------------
         * redis常用指令：针对Value的操作【Hash类型】
         * （1）HSET key field value:设置给某个key的某个field(字段)一个字段值
         * （2）hexists key field：判断指定的key中的filed是否存在，存在返回1，不存在返回0
         * （3）hincrby key field increment：给某个key的某个field对应的值增加指定的数值
         *                                  例如 hincrby user age 2，将用户的年龄加2
         * ------------------------------------------------------------------------
         */
        /* 1、第一步：定义lua脚本
        ----------------------------------------------------------------------------------------
        if(redis.call('exists', KEYS[1]) == 0 or redis.call('hexists', KEYS[1], KEYS[2]) == 1)
		then
			redis.call('hincrby', KEYS[1], KEYS[2], 1)
			redis.call('expire', KEYS[1], ARGV[1])
			return 1
		else
			return 0
		end
        ----------------------------------------------------------------------------------------
        redis.call，表示执行redis的指令，exists、hexists、hincrb、expire都是redis的指令
        KEYS: KEYS[1]表示锁的key，如“lock”、 KEYS[2]表示uuid，如“2323-2322-1212-2323”
		ARGV: ARGV[1]，表示设置的过期时间，如30
		----------------------------------------------------------------------------------------
		lua脚本获取锁的步骤：
		（1）：
		判断lock锁是否存在，执行的指令是exists lockKey，判断是否等于0，等于0则不存在
		（2）：
		如果lock锁不存在，则直接获取锁，执行的指令是hincrby lockKey uuid 1，也就是设置值为1
        （3）：
        如果lock锁存在，再判断是不是自己当前线程的锁，执行的指令是hexists lockKey uuid，判断是否等于1，等于1则是
        （4）：
        如果是自己当前线程的锁，为锁设置过期时间
        （5）：
        如果返回值是0代表不是自己的锁，直接返回return 0
         */
        String script = "if(redis.call('exists', KEYS[1]) == 0 or redis.call('hexists', KEYS[1], KEYS[2]) == 1) then redis.call('hincrby', KEYS[1], KEYS[2], 1) redis.call('expire', KEYS[1], ARGV[1]) return 1 else return 0 end";

        //（2）第二步：执行lua脚本
        Boolean flag = this.stringRedisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName, uuid), expire.toString());

        //（3）对返回结果进行判断
        if (!flag) {
            try {
                //如果没有获取到锁，则不断递归调用，不断重试获取锁
                Thread.sleep(200);
                tryLock(lockName, uuid, expire);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 如果获取到锁，自动续期
        this.renewExpire(lockName, uuid, expire * 1000);

        // 获取到锁，返回true
        return true;
    }

    /**
     * 释放锁
     */
    public void unlock(String lockName, String uuid) {
        /*（1）定义lua脚本
        1.判断自己的lock锁是否存在（hexists lock uuid），如果返回值是0，代表锁不存在或者不是自己的锁 。return nil
        2.如果返回值是1，说明自己的锁存在。hincrby lock uuid -1，减完之后判断返回值，如果是0，则删除锁（del lock）
        3.否则直接返回0 return 0
         */
        String script = "if(redis.call('hexists', KEYS[1], KEYS[2]) == 0) then return nil elseif(redis.call('hincrby', KEYS[1], KEYS[2], -1) == 0) then return redis.call('del', KEYS[1]) else return 0 end";

        //（2）执行lua脚本
        Long flag = this.stringRedisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(lockName, uuid));

        //（3）如果没有返回值，说明不是锁不存在或者不是自己的锁，尝试解其他线程的锁，抛出异常
        if (flag == null) {
            throw new IllegalMonitorStateException("attempt to unlock lock, not locked by lockName: "
                    + lockName + " with request: " + uuid);
        } else if (flag == 1) {
            // 当返回值是0时，说明锁已经存在，删除自动续期定时任务
            timer.cancel();
        }
    }

    /**
     * 自动续期定时任务
     * 判断自己的锁是否存在，如果存在才能续期
     */
    private void renewExpire(String lockName, String uuid, Long expire) {
        //（1）声明lua脚本
        String script = "if(redis.call('hexists', KEYS[1], KEYS[2]) == 1) then return redis.call('expire', 'lock', ARGV[1]) else return 0 end";

        //（2）创建看门狗定时器，提升作用域
        timer = new Timer();
        //（3）schedule方法，表示安排指定的任务在指定的延迟后执行
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // 执行lua脚本
                stringRedisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName, uuid), expire.toString());
            }
            // 设置自动续期的时间，到达原来的过期时间的3分之一时，自动续期，单位毫秒，故乘以1000
        }, expire * 1000 / 3, expire * 1000 / 3);

    }
}
