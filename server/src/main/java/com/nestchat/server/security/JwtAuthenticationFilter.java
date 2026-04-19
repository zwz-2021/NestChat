package com.nestchat.server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nestchat.server.common.Result;
import com.nestchat.server.common.ResultCode;
import com.nestchat.server.common.TraceIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationFilter(JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth/") || path.startsWith("/static/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 生成 traceId
        TraceIdUtil.set(TraceIdUtil.generate());

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(response, ResultCode.UNAUTHORIZED);
            return;
        }

        String token = authHeader.substring(7);

        // 检查黑名单
        Boolean blacklisted = redisTemplate.hasKey("token:blacklist:" + token);
        if (Boolean.TRUE.equals(blacklisted)) {
            writeError(response, ResultCode.UNAUTHORIZED);
            return;
        }

        try {
            String userId = jwtUtil.parseUserId(token);
            UserContext.set(userId);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            writeError(response, ResultCode.UNAUTHORIZED);
        } finally {
            UserContext.clear();
            TraceIdUtil.clear();
        }
    }

    private void writeError(HttpServletResponse response, ResultCode code) throws IOException {
        response.setStatus(200);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(code)));
    }
}
