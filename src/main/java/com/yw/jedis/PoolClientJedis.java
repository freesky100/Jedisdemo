package com.yw.jedis;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.*;
import redis.clients.jedis.params.sortedset.ZAddParams;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 连接池
 * Created by yw on 2018/3/22.
 */
@Slf4j
public class PoolClientJedis {

    private final static String HOST ="192.168.1.201";
    private final static String AUTH="yw";
    private static final int PORT=6379;
    private static JedisPool jedisPool = null;

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
        jedisPool = new JedisPool(jedisPoolConfig,HOST,PORT,3000,AUTH);
    }


    private static synchronized Jedis getJedis(){
        Jedis jedis = null;
        try {
            if (jedisPool != null) {
                jedis = jedisPool.getResource();
                log.debug("jedis对象:{}", jedis);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return jedis;
    }

    public static void close(final Jedis jedis){
        if(jedis!=null)
            jedis.close();
    }


    /**
     * 操作String
     * @param jedis jedis object
     */
    public static void TestString(Jedis jedis) throws InterruptedException {
        jedis.set("java","hello World");

        boolean exist = jedis.exists("java");
        log.debug("是否存在java键：{}",exist);

        String value = jedis.get("java");
        log.debug("获取java值:{}",value);

        long res = jedis.append("java"," java");
        log.debug("append java值:{},返回long:{}",jedis.get("java"),res);

        jedis.del("java");
        log.debug("DEL 获取java值:{}",jedis.get("java"));

        jedis.mset("java","hello","c","world");
        log.debug("获取java值:{},c值{}",jedis.get("java"),jedis.get("c"));
//        Thread.sleep(2000);
        close(jedis);
    }

    public static void TestHash(final Jedis jedis){
        Map<String ,String> map = new HashMap<>();
        map.put("name","韩雪");
        map.put("age","30");
        map.put("show","声临其境");
        jedis.hmset("girls",map);
        map = jedis.hgetAll("girls");
        log.debug("获取map值：{}",map);

        jedis.hset("girls","name","李冰冰");
        jedis.hset("girls","age","35");
        jedis.hset("girls","show","巢穴");
        map = jedis.hgetAll("girls");
        log.debug("获取map值：{}",map);

        long len = jedis.hlen("girls");
        boolean exist = jedis.exists("girls");
        Set<String> set= jedis.hkeys("girls");
        List<String> vals = jedis.hvals("girls");
        log.debug("获取信息:，存在：{},长度:{},keys列表：{}，vals列表：{}",exist,len,set,vals);
        close(jedis);
    }

    /**
     * 操作Set
     * @param jedis jedis object
     */
    public static void TestSortSet(final Jedis jedis){
        jedis.zadd("book",10,"java编程思想");
        jedis.zadd("book",14.8,"c编程思想", ZAddParams.zAddParams().nx());
        formatSet(jedis,"book");
//        formatSet(jedis.zrangeWithScores("book",0,-1));
//        formatSet(jedis.zrangeByScoreWithScores("book",0,20));

        Map<String,Double> map = new HashMap<>();
        map.put("Python编程思想",5.8);
        map.put("C#编程思想",1.2);
        jedis.zadd("book1",map);
        formatSet(jedis,"book1");
        ZParams zParams = new ZParams();
        zParams.aggregate(ZParams.Aggregate.MAX);
        zParams.weightsByDouble(1,2.0);
        jedis.zunionstore("unionbooks",zParams,"book","book1");
        formatSet(jedis,"unionbooks");
        close(jedis);
    }

    /**
     * 打印直观
     */
    private static void formatSet(Set<Tuple> set){
        set.forEach(e-> log.debug("返回的sortedSet:"+e.getElement()+":"+e.getScore()));
    }

    private static void formatSet(Jedis jedis,String key){
        jedis.zrangeWithScores(key,0,-1).forEach(e->log.debug(key+"->返回的sortedSet:"+e.getElement()+":"+e.getScore()));
    }


    public static void main(String[] args) throws InterruptedException {
        log.debug("连接池方式连接redis");
        //设置连接池参数


        //没有锁的情况下或redis事务下，并发值会很有意思
        for(int i = 0;i<100;i++){
            log.debug("执行第"+i+"次");
            Jedis jedis = getJedis();
           new Thread(new ThreadDo(jedis)).start();
        }

        Jedis jedis;
        jedis = getJedis();
        TestHash(jedis);
        jedis = getJedis();//此处注释掉后，发现redis并不是立即释放
        TestSortSet(jedis);
        //释放连接池
//        jedisPool.returnResource(jedis);//已过期


        jedisPool.close();
    }
}

@Data
class ThreadDo implements Runnable{
    private Jedis jedis;

    public ThreadDo(Jedis jedis) {
        this.jedis = jedis;
    }

    @Override
    public void run() {
        try {
            System.out.println(Thread.currentThread().getName()+"-->"+jedis);
            Thread.sleep(1000);
            PoolClientJedis.TestString(jedis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
