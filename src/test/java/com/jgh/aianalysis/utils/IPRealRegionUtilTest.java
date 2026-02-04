package com.jgh.aianalysis.utils;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties = {
        "dubbo.protocol.port=-1" // 使用随机端口
})
class IPRealRegionUtilTest {

    @Test
    void getRegion() {
        // 中国|广东省|深圳市|电信|CN----113.92.157.29
        //  192.168.2.7--Reserved|Reserved|Reserved|0|0
        String region = IPRealRegionUtil.getRegion("192.168.2.7");
        System.out.println(region);
    }
}