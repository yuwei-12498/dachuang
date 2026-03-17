package com.citytrip;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.citytrip.mapper")
public class CityTripApplication {
    public static void main(String[] args) {
        SpringApplication.run(CityTripApplication.class, args);
    }
}
