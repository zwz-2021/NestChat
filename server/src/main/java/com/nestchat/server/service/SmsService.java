package com.nestchat.server.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "sms.enabled", havingValue = "true", matchIfMissing = false)
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    @Value("${sms.access-key-id}")
    private String accessKeyId;

    @Value("${sms.access-key-secret}")
    private String accessKeySecret;

    @Value("${sms.sign-name}")
    private String signName;

    @Value("${sms.template-code}")
    private String templateCode;

    /**
     * 发送短信验证码
     * @param phoneNumber 手机号
     * @param code 验证码
     * @return 是否发送成功
     */
    public boolean sendVerificationCode(String phoneNumber, String code) {
        try {
            Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret)
                    .setEndpoint("dysmsapi.aliyuncs.com");

            Client client = new Client(config);

            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phoneNumber)
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setTemplateParam("{\"code\":\"" + code + "\"}");

            SendSmsResponse response = client.sendSms(request);

            if (response.getBody() != null && response.getBody().getCode() != null) {
                String responseCode = response.getBody().getCode();
                String message = response.getBody().getMessage();

                if ("OK".equals(responseCode)) {
                    log.info("短信发送成功: 手机号={}, 验证码={}", phoneNumber, code);
                    return true;
                } else {
                    log.error("短信发送失败: code={}, message={}", responseCode, message);
                    return false;
                }
            } else {
                log.error("短信发送失败: 响应为空");
                return false;
            }

        } catch (Exception e) {
            log.error("短信发送异常: ", e);
            return false;
        }
    }

    /**
     * 发送短信（通用方法）
     * @param phoneNumber 手机号
     * @param templateCode 模板代码
     * @param templateParam 模板参数
     * @return 是否发送成功
     */
    public boolean sendSms(String phoneNumber, String templateCode, String templateParam) {
        try {
            Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret)
                    .setEndpoint("dysmsapi.aliyuncs.com");

            Client client = new Client(config);

            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phoneNumber)
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setTemplateParam(templateParam);

            SendSmsResponse response = client.sendSms(request);

            if (response.getBody() != null && response.getBody().getCode() != null) {
                String responseCode = response.getBody().getCode();
                if ("OK".equals(responseCode)) {
                    log.info("短信发送成功: 手机号={}", phoneNumber);
                    return true;
                }
            }
            return false;

        } catch (Exception e) {
            log.error("短信发送异常: ", e);
            return false;
        }
    }
}
