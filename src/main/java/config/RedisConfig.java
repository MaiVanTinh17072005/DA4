package config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Redis configuration using Jedis
 */
@Configuration
public class RedisConfig {
    
    @Value("${redis.host:localhost}")
    private String redisHost;
    
    @Value("${redis.port:6379}")
    private int redisPort;
    
    @Bean
    public JedisPool jedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(2);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        
        System.out.println("[RedisConfig] Creating JedisPool for " + redisHost + ":" + redisPort);
        
        return new JedisPool(poolConfig, redisHost, redisPort);
    }
}
