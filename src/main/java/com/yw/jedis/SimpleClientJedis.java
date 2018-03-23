package com.yw.jedis;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by yw on 2018/3/22.
 */
@Slf4j
public class SimpleClientJedis {

    private final static String ip ="192.168.1.201";
    private final static String auth="yw";

//    private static  Logger logger = LoggerFactory.getLogger(SimpleClientJedis.class);
//    private final static int port = "6379";
    public static void main(String[] args) {
        log.debug("jedis应用....");
        Jedis jedis = new Jedis(ip);
        jedis.auth(auth);
        Set<String> set = jedis.keys("*");
        System.out.println(set);
        log.info("对象：{}",set);
        //no .1 查询key
        Iterator iterator = set.iterator();
        while(iterator.hasNext()){
            String value = (String)iterator.next();
            System.out.println("Set key:"+value);
        }

        //no .2 打印String类型值
        set.stream().forEach(e-> {if(jedis.type(e).equalsIgnoreCase("String"))System.out.println(jedis.get(e));});

        //no .3 操作list类型
        long listLength =jedis.llen("list");
        List<String> list = jedis.lrange("list",0,-1);
        log.debug("初始list长度：{},list内容：{}",listLength,list);
        jedis.lpush("list","爱迪生");
        jedis.rpush("list","拉斐尔");
       list = jedis.lrange("list",0,-1);
       log.debug("操作后list长度：{},list内容：{}",listLength,list);
    }
}
