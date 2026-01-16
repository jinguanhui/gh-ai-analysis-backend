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
        Boolean b = smsService.sendLoginCode("15180091776");
        System.out.println(b);
    }

//    @Test
//    void verifyCode() {
//        Boolean b = smsService.verifyCode("15180091776", "0577");
//        System.out.println(b);
//    }
}