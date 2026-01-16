package com.jgh.aianalysis.controller;

import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.service.SmsService;
import com.jgh.ghcommon.common.BaseResponse;
import com.jgh.ghcommon.model.dto.sms.SmsCodeSendDTO;
import com.jgh.ghcommon.model.dto.sms.SmsCodeVerifyDTO;
import com.jgh.ghcommon.model.entity.User;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sms")
@RequiredArgsConstructor
@Slf4j
public class SmsController {

    private final SmsService smsService;

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
