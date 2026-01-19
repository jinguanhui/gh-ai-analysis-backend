package com.jgh.aianalysis.utils;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TextGreenUtilsTest {

    @Resource
    private TextGreenUtils textGreenUtils;

    @Test
    void greenTextScanPlusVersion() {

        try {
            Map map = textGreenUtils.greenTextScanPlusVersion("你好，666");
            System.out.println(map.toString());
        } catch (Exception e) {
            System.out.println("操作错误");
            throw new RuntimeException(e);
        }
    }
}