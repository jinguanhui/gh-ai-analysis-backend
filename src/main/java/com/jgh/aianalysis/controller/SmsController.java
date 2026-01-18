package com.jgh.aianalysis.controller;

import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.service.SmsService;
import com.jgh.aianalysis.utils.MailMsgUtil;
import com.jgh.ghcommon.common.BaseResponse;
import com.jgh.ghcommon.model.dto.sms.MailCodeSendDTO;
import com.jgh.ghcommon.model.dto.sms.MailCodeVerifyDTO;
import com.jgh.ghcommon.model.dto.sms.SmsCodeSendDTO;
import com.jgh.ghcommon.model.dto.sms.SmsCodeVerifyDTO;
import com.jgh.ghcommon.model.entity.User;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
@Slf4j
public class SmsController {

    private final SmsService smsService;
    private final MailMsgUtil mailMsgUtil;

    @PostMapping("/mail/send")
    public BaseResponse<Boolean> send(@RequestBody MailCodeSendDTO dto) {
        log.info("正在进行邮件发送");
        try {
            return BaseResponse.success(mailMsgUtil.mail(dto.getEmail()));
        } catch (MessagingException e) {
            throw new BusinessException("邮件发送失败！！！");
        }
    }

    @PostMapping("/mail/verify")
    public BaseResponse<User> verify(@RequestBody MailCodeVerifyDTO dto, HttpServletRequest request, HttpServletResponse response) {
        log.info("正在进行邮件验证");
        return BaseResponse.success(smsService.verify(dto.getEmail(), dto.getCode(), request, response));
    }



    @PostMapping("/send")
    public BaseResponse<Boolean> sendCode(@RequestBody @Valid SmsCodeSendDTO dto) {
        log.info("正在进行短信发送");
        return BaseResponse.success(smsService.sendLoginCode(dto.getPhone()));
    }

    @PostMapping("/verify")
    public BaseResponse<User> verify(@RequestBody @Valid SmsCodeVerifyDTO dto, HttpServletRequest request, HttpServletResponse response) {
        log.info("正在进行短信验证");
        User user = smsService.verifyCode(dto.getPhone(), dto.getCode(), request, response);

        if (user == null) {
            throw new BusinessException("验证码验证失败");
        }

        return BaseResponse.success(user);
    }
}
