package com.jgh.aianalysis.mq;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static com.jgh.ghcommon.constant.CommonConstant.EXCHANGE_NAME;
import static com.jgh.ghcommon.constant.CommonConstant.ROUTING_KEY;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "dubbo.protocol.port=-1" // 使用随机端口
})
class MyMessageProducerTest {

    @Resource
    private MyMessageProducer myMessageProducer;

    @Test
    void sendMessage() {
//        myMessageProducer.sendMessage(EXCHANGE_NAME, ROUTING_KEY, "hello world");
    }
}