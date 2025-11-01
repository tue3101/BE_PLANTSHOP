package com.example.backendplantshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.example.backendplantshop.mapper")
public class BackendPlantshopApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendPlantshopApplication.class, args);
    }

}
