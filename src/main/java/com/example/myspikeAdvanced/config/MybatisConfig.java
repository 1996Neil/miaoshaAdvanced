package com.example.myspikeAdvanced.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName MybatisConfig
 * @create 2021-08-03 21:48
 * @description
 */
@Configuration
@MapperScan("com.example.myspikeAdvanced.mbg")
public class MybatisConfig {
}
