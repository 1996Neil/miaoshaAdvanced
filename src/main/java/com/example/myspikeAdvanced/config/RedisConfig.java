package com.example.myspikeAdvanced.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName RedisConfig
 * @create 2021-08-12 16:48
 * @description
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600)
public class RedisConfig {
}
