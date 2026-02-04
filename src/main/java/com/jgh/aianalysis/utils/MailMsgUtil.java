package com.jgh.aianalysis.utils;
 
 
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
 
import java.time.Duration;
 
/**
 * @author young
 * @date 2022/12/7 18:41
 * @description: 发送邮箱业务
 */
@Component
public class MailMsgUtil {
 
    @Resource
    private JavaMailSenderImpl mailSender;
    @Resource
    private RedisUtil redisUtil;

    @Value("${spring.mail.username}")
    private String senderMail;
 
    public boolean mail(String email) throws MessagingException {
 
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        //生成随机验证码
        String code = CodeGeneratorUtil.generateCode(6);
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        //设置一个html邮件信息
        helper.setText("<p style='color: blue'>GH智能分析平台！你的验证码为：" + code + "(有效期为一分钟)</p>", true);
        //设置邮件主题名
        helper.setSubject("GH智能分析平台验证码----验证码");
        //发给谁-》邮箱地址
        helper.setTo(email);
        //谁发的-》发送人邮箱
        helper.setFrom(senderMail);
        //将邮箱验证码以邮件地址为key存入redis,1分钟过期
        redisUtil.set(email, code, 60);
        mailSender.send(mimeMessage);
        return true;
    }

    public boolean mailLoginMessage(String email,String ip, String loginDate) throws MessagingException {

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        //生成随机验证码
        String code = CodeGeneratorUtil.generateCode(6);
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        //设置一个html邮件信息
        helper.setText("<p style='color: blue'>GH智能分析平台！你的账号存在异地登录!!!登录时间为：" + loginDate + "登录IP为：" + ip + "</p>", true);
        //设置邮件主题名
        helper.setSubject("GH智能分析平台异地登录提醒！！！");
        //发给谁-》邮箱地址
        helper.setTo(email);
        //谁发的-》发送人邮箱
        helper.setFrom(senderMail);
        mailSender.send(mimeMessage);
        return true;
    }
}