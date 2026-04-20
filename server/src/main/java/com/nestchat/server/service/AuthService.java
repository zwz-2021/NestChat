package com.nestchat.server.service;

import com.nestchat.server.common.BusinessException;
import com.nestchat.server.common.IdGenerator;
import com.nestchat.server.common.ResultCode;
import com.nestchat.server.dto.request.LoginRequest;
import com.nestchat.server.dto.request.RegisterRequest;
import com.nestchat.server.dto.request.ResetPasswordRequest;
import com.nestchat.server.dto.request.SendResetCodeRequest;
import com.nestchat.server.dto.response.CaptchaResponse;
import com.nestchat.server.dto.response.LoginResponse;
import com.nestchat.server.dto.response.UserBriefResponse;
import com.nestchat.server.entity.User;
import com.nestchat.server.mapper.UserMapper;
import com.nestchat.server.security.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final long CAPTCHA_TTL_SECONDS = 300;

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SmsService smsService;

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil, StringRedisTemplate redisTemplate,
                      @Autowired(required = false) SmsService smsService) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.smsService = smsService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public CaptchaResponse generateCaptcha(String type) {
        String captchaId = "c_" + type + "_" + Integer.toHexString(ThreadLocalRandom.current().nextInt(0x10000000, 0x7FFFFFFF));
        String code = randomCode(4);

        // 存到 Redis
        redisTemplate.opsForValue().set("captcha:" + captchaId, code, CAPTCHA_TTL_SECONDS, TimeUnit.SECONDS);

        // 生成验证码图片
        String imageBase64 = generateCaptchaImage(code);

        CaptchaResponse resp = new CaptchaResponse();
        resp.setCaptchaId(captchaId);
        resp.setImageBase64("data:image/png;base64," + imageBase64);
        resp.setExpireAt(System.currentTimeMillis() + CAPTCHA_TTL_SECONDS * 1000);
        return resp;
    }

    public LoginResponse login(LoginRequest req) {
        // 校验验证码
        validateCaptcha(req.getCaptchaId(), req.getCaptchaCode());

        // 查找用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getAccount, req.getAccount()));
        if (user == null) {
            throw new BusinessException(ResultCode.AUTH_ERROR);
        }

        // 校验密码
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.AUTH_ERROR);
        }

        // 生成 token
        String accessToken = jwtUtil.createAccessToken(user.getUserId());
        String refreshToken = jwtUtil.createRefreshToken(user.getUserId());

        LoginResponse resp = new LoginResponse();
        resp.setAccessToken(accessToken);
        resp.setRefreshToken(refreshToken);
        resp.setExpireAt(jwtUtil.getAccessExpireAtMillis());

        UserBriefResponse userBrief = new UserBriefResponse();
        userBrief.setUserId(user.getUserId());
        userBrief.setAccount(user.getAccount());
        userBrief.setNickname(user.getNickname());
        userBrief.setAvatarUrl(user.getAvatarUrl());
        resp.setUser(userBrief);

        return resp;
    }

    public void register(RegisterRequest req) {
        // 校验验证码
        validateCaptcha(req.getCaptchaId(), req.getCaptchaCode());

        // 校验两次密码
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_MISMATCH);
        }

        // 检查账号唯一性
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getAccount, req.getAccount()));
        if (count > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_REGISTRATION);
        }

        // 创建用户
        User user = new User();
        user.setUserId(IdGenerator.generate("u"));
        user.setAccount(req.getAccount());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname("用户" + req.getAccount().substring(req.getAccount().length() - 4));
        user.setAvatarUrl("");
        user.setMoodCode("");
        user.setMoodText("");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
    }

    public void sendResetCode(SendResetCodeRequest req) {
        // 检查账号存在
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getAccount, req.getAccount()));
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "账号不存在");
        }

        // 生成6位验证码
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        redisTemplate.opsForValue().set("reset_code:" + req.getAccount(), code, CAPTCHA_TTL_SECONDS, TimeUnit.SECONDS);

        // 尝试发送短信
        boolean smsSent = false;
        if (smsService != null) {
            smsSent = smsService.sendVerificationCode(req.getAccount(), code);
        }

        // 无论短信是否发送成功，都打印到日志
        log.info("========== 密码重置验证码 ==========");
        log.info("账号: {}, 验证码: {}, 短信发送: {}", req.getAccount(), code, smsSent ? "成功" : "失败（未配置或配置错误）");
        log.info("====================================");
    }

    public void resetPassword(ResetPasswordRequest req) {
        // 校验验证码
        String key = "reset_code:" + req.getAccount();
        String storedCode = redisTemplate.opsForValue().get(key);
        if (storedCode == null) {
            throw new BusinessException(ResultCode.CAPTCHA_EXPIRED);
        }
        if (!storedCode.equals(req.getVerifyCode())) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR);
        }
        redisTemplate.delete(key);

        // 校验两次密码
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_MISMATCH);
        }

        // 更新密码
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getAccount, req.getAccount()));
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "账号不存在");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public void logout(String token) {
        long remaining = jwtUtil.getRemainingMillis(token);
        if (remaining > 0) {
            redisTemplate.opsForValue().set("token:blacklist:" + token, "1", remaining, TimeUnit.MILLISECONDS);
        }
    }

    private void validateCaptcha(String captchaId, String captchaCode) {
        String key = "captcha:" + captchaId;
        String storedCode = redisTemplate.opsForValue().get(key);
        if (storedCode == null) {
            throw new BusinessException(ResultCode.CAPTCHA_EXPIRED);
        }
        if (!storedCode.equalsIgnoreCase(captchaCode)) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR);
        }
        redisTemplate.delete(key);
    }

    private String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        Random r = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            sb.append(CAPTCHA_CHARS.charAt(r.nextInt(CAPTCHA_CHARS.length())));
        }
        return sb.toString();
    }

    private String generateCaptchaImage(String code) {
        int width = 120, height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        Random r = ThreadLocalRandom.current();

        // 背景
        g.setColor(new Color(240, 240, 240));
        g.fillRect(0, 0, width, height);

        // 干扰线
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(r.nextInt(200), r.nextInt(200), r.nextInt(200)));
            g.drawLine(r.nextInt(width), r.nextInt(height), r.nextInt(width), r.nextInt(height));
        }

        // 文字
        g.setFont(new Font("Arial", Font.BOLD, 28));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(r.nextInt(150), r.nextInt(150), r.nextInt(150)));
            double angle = (r.nextDouble() - 0.5) * 0.4;
            g.rotate(angle, 25 + i * 25, 28);
            g.drawString(String.valueOf(code.charAt(i)), 10 + i * 25, 30);
            g.rotate(-angle, 25 + i * 25, 28);
        }

        // 噪点
        for (int i = 0; i < 30; i++) {
            image.setRGB(r.nextInt(width), r.nextInt(height),
                    new Color(r.nextInt(256), r.nextInt(256), r.nextInt(256)).getRGB());
        }

        g.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("生成验证码图片失败", e);
        }
    }
}
