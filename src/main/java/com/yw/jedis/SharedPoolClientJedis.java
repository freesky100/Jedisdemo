package com.yw.jedis;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * 前段分片
 * 2个redis为单独启动且都为master
 * Created by yw on 2018/3/23.
 */
@Slf4j
public class SharedPoolClientJedis {

    private final static String HOST="192.168.1.201";
    private final static String AUTH="yw";
    private static final int PORT=6379;
    private static final int PORT1=6380;
    private static ShardedJedisPool shardedJedisPool;

    //初始化连接池
    static{
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(50);
        // 连接耗尽时是否阻塞, false报异常,ture阻塞直到超时, 默认tr
        jedisPoolConfig.setBlockWhenExhausted(true);
        // 设置的逐出策略类名, 默认DefaultEvictionPolicy(当连接超过最大空闲时间,或连接数超过最大空闲连接数)
        jedisPoolConfig.setEvictionPolicyClassName("org.apache.commons.pool2.impl.DefaultEvictionPolicy");
        //jmx管理功能，默认true
        jedisPoolConfig.setJmxEnabled(true);
        // 最大空闲连接数, 默认8个 控制一个pool最多有多少个状态为idle(空闲的)的jedis实例。
        jedisPoolConfig.setMaxIdle(5);
        // 表示当borrow(引入)一个jedis实例时，最大的等待时间，如果超过等待时间，则直接抛出JedisConnectionException；
        jedisPoolConfig.setMaxWaitMillis(1000*100);
        // 在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
        jedisPoolConfig.setTestOnBorrow(true);

        List<JedisShardInfo> list = new ArrayList<>();
        JedisShardInfo jedisShardInfo1 = new JedisShardInfo(HOST,PORT);
        jedisShardInfo1.setPassword(AUTH);
        JedisShardInfo jedisShardInfo2 = new JedisShardInfo(HOST,PORT1);
        jedisShardInfo2.setPassword(AUTH);
        list.add(jedisShardInfo1);
        list.add(jedisShardInfo2);
        shardedJedisPool = new ShardedJedisPool(jedisPoolConfig,list);
    }


    private static synchronized ShardedJedis getJedis(){
        ShardedJedis jedis = null;
        try {
            if (shardedJedisPool != null) {
                jedis = shardedJedisPool.getResource();
                log.debug("jedis对象:{}", jedis);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return jedis;
    }

    private static void close(ShardedJedis jedis){
        if(jedis!=null){
            jedis.close();
        }
    }

    public static void TestString(ShardedJedis jedis) throws InterruptedException {
        jedis.set("java","hello World");

        boolean exist = jedis.exists("java");
        log.debug("是否存在java键：{}",exist);

        String value = jedis.get("java");
        log.debug("获取java值:{}",value);

        long res = jedis.append("java"," java");
        log.debug("append java值:{},返回long:{}",jedis.get("java"),res);

        jedis.del("java");
        log.debug("DEL 获取java值:{}",jedis.get("java"));

//        jedis.mset("java","hello","c","world");
        log.debug("获取java值:{},c值{}",jedis.get("java"),jedis.get("c"));
//        Thread.sleep(2000);
        close(jedis);
    }


    /**
     * 线程安全
     * @throws InterruptedException
     */
    public static void testThreadSafe() throws InterruptedException {
        AtomicInteger n = new AtomicInteger(100);
        Thread.sleep(1000);
        while(n.decrementAndGet()>0){
            new Thread(()->{
                ShardedJedis jedis  = shardedJedisPool.getResource();
                log.info("test={}",jedis.set("test","n:"+n.get()));
                jedis.close();
            }).start();
        }
    }


    /**
     * jedis本身线程不安全
     * @throws InterruptedException
     */
    public static void testThreadNotSafe() throws  InterruptedException{
        AtomicInteger n = new AtomicInteger(100);
        Thread.sleep(1000);
        ShardedJedis jedis = shardedJedisPool.getResource();
        while (n.decrementAndGet()>0){
            new Thread(()->{
                log.info("test={}",jedis.set("test1","n"+n.get()));
                jedis.close();
            }
        ).start();
        }
    }


    public static void main(String[] args) throws InterruptedException {
        ShardedJedis jedis = getJedis();
        TestString(jedis);
        testThreadSafe();
        testThreadNotSafe();
    }

}
