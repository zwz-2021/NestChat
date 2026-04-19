package com.nestchat.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.nestchat.server.mapper")
public class NestChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(NestChatApplication.class, args);
    }
}
