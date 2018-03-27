package com.yw.jedis;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.util.HashSet;
import java.util.Set;

/**
 * 后台集群cluster
 * Created by yw on 2018/3/23.
 */
@Slf4j
public class ClusterClientJedis {

    private static JedisCluster jedisCluster;

    static{
        Set<HostAndPort> jedisClusterNodes  = new HashSet<>();
        jedisClusterNodes.add(new HostAndPort("192.168.1.201",6379));
        jedisClusterNodes.add(new HostAndPort("192.168.1.202",6380));
        jedisClusterNodes.add(new HostAndPort("192.168.1.203",6381));
         jedisCluster = new JedisCluster(jedisClusterNodes);
    }


    private static void TestString(){
        log.debug("开始测试String");
        jedisCluster.set("java","hello world!");
        String val = jedisCluster.get("java");
        log.debug("当前key:java的val:{}",val);

    }

    public static void main(String[] args) {
        TestString();
    }

}
