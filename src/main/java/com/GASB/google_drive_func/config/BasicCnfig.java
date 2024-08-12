package com.GASB.google_drive_func.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class BasicCnfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
