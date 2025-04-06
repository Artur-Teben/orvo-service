package com.orvo.emailgenerator;

import com.orvo.emailgenerator.config.property.EmailProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(EmailProperties.class)
public class OrvoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrvoServiceApplication.class, args);
    }

}
