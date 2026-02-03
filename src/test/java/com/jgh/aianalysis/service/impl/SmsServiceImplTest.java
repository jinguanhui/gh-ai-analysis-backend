package com.jgh.aianalysis.service.impl;

import com.jgh.aianalysis.service.SmsService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SmsServiceImplTest {

    @Resource
    private SmsService smsService;

    @Test
    void sendLoginCode() {
        Boolean b = smsService.sendLoginCode("15180091776", "100002");
        System.out.println(b);
    }

    @Test
    void verifyCode() {
//        Boolean b = smsService.verifyCodeUpdatePhone("15180091776", "0571");
//        System.out.println(b);
    }
}