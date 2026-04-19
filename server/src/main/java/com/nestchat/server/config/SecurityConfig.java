package com.nestchat.server.config;

import com.nestchat.server.security.JwtAuthenticationFilter;
import com.nestchat.server.security.JwtUtil;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class SecurityConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilter(JwtUtil jwtUtil,
                                                                      StringRedisTemplate redisTemplate) {
        FilterRegistrationBean<JwtAuthenticationFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new JwtAuthenticationFilter(jwtUtil, redisTemplate));
        reg.addUrlPatterns("/*");
        reg.setOrder(1);
        return reg;
    }
}
