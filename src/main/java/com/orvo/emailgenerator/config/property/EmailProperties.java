package com.orvo.emailgenerator.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "orvo.email")
public class EmailProperties {

    private int port;
    private String username;
    private Smtp smtp;

    @Data
    public static class Smtp {

        private int connectiontimeout;
        private int timeout;
        private int writetimeout;

    }

}
