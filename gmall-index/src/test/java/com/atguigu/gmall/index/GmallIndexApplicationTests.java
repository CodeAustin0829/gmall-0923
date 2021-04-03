package com.atguigu.gmall.index;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GmallIndexApplicationTests {

    @Autowired
    private RedissonClient redissonClient;
    @Test
    void contextLoads() {
    }

    @Test
    public void testRedissonBloom(){
        RBloomFilter<String> bloom = this.redissonClient.getBloomFilter("bloom");
        //插入20条数据，误判率是0.03
        bloom.tryInit(20, 0.03);
        bloom.add("1");
        bloom.add("2");
        bloom.add("3");
        bloom.add("4");
        bloom.add("5");
        System.out.println(bloom.contains("1"));
        System.out.println(bloom.contains("3"));
        System.out.println(bloom.contains("5"));
        System.out.println(bloom.contains("6"));
        System.out.println(bloom.contains("7"));
        System.out.println(bloom.contains("8"));
        System.out.println(bloom.contains("9"));
        System.out.println(bloom.contains("10"));
        System.out.println(bloom.contains("11"));
        System.out.println(bloom.contains("12"));
        System.out.println(bloom.contains("13"));
        System.out.println(bloom.contains("14"));
        System.out.println(bloom.contains("15"));
        System.out.println(bloom.contains("16"));
    }

}
