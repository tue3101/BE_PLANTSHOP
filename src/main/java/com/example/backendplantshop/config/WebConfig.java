package com.example.backendplantshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**") //Khai báo pattern URL.
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/images/"); //Chỉ định thư mục vật lý chứa file.
    }
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
