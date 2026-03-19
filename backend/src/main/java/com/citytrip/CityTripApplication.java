package com.citytrip;

import com.citytrip.config.LlmProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.citytrip.mapper")
@EnableConfigurationProperties(LlmProperties.class)
public class CityTripApplication {
    public static void main(String[] args) {
        SpringApplication.run(CityTripApplication.class, args);
    }
}
