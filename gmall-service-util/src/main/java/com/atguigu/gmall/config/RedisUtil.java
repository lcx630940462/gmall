package com.atguigu.gmall.config;

import org.apache.catalina.Host;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtil {

    //配置连接池的参数 连接池对象
    private JedisPool jedisPool;

    /**
     *
     * @param host      ip地址
     * @param port      端口号
     * @param database  指定哪个库
     */
    public void  initJedisPoll(String host,int port,int database){
        //创建jedisPoolConfig
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        //总数
        jedisPoolConfig.setMaxTotal(200);
        //设置连接时等待的最大毫秒
        jedisPoolConfig.setMaxWaitMillis(10*1000);
        //最少剩余数
        jedisPoolConfig.setMinIdle(10);
        //如果到最大 设置等待
        jedisPoolConfig.setBlockWhenExhausted(true);
        //等待时间
        jedisPoolConfig.setMaxWaitMillis(2000);
        //在读取连接时，检查是否有效
        jedisPoolConfig.setTestOnBorrow(true);

         jedisPool= new JedisPool(jedisPoolConfig, host, port, 20 * 1000);


    }

    public  Jedis getJedis(){
        Jedis jedis = jedisPool.getResource();
        return jedis;
    }
}
