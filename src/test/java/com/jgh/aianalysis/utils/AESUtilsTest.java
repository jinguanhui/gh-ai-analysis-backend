package com.jgh.aianalysis.utils;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

//@SpringBootTest
class AESUtilsTest {

//    @Test
//    void encodeByECB() throws Exception {
//        System.out.println("------------------AES加密之ECB加密转base64------------------");
//        //key的长度至少为24位并且是8的倍数（比如32，40等等）
//        String aesKey = "ee1b7d9ca0b7d10d2cd27f35d34bb412";
//        String plainText = "<root><out_refund_no>20180608163757915497</out_refund_no><out_trade_no>15070411211528250141228</out_trade_no><refund_account>REFUND_SOURCE_RECHARGE_FUNDS</refund_account><refund_fee>1</refund_fee><refund_id>50000607002018060805036784330</refund_id><refund_recv_accout>支付用户零钱</refund_recv_accout><refund_request_source>API</refund_request_source><refund_status>SUCCESS</refund_status><settlement_refund_fee>1</settlement_refund_fee><settlement_total_fee>1</settlement_total_fee><success_time>2018-06-08 16:38:05</success_time><total_fee>1</total_fee><transaction_id>4200000129201806063432463966</transaction_id></root>";
//        System.out.println("ECB加密原始数据：" + plainText);
//        byte[] key = aesKey.getBytes(StandardCharsets.UTF_8);
//        byte[] data = plainText.getBytes(StandardCharsets.UTF_8);
//        //ECB模式加密
//        byte[] encode = AESUtils.encodeByECB(key, data);
//        System.out.println("ECB加密后转Base64的数据：" + Base64.getEncoder().encodeToString(encode));
//        System.out.println("b2dc5c794468732d30e747d1eadc1d32".length());
//    }
//
//    @Test
//    void decodeByECB() throws Exception {
//        System.out.println("------------------AES加密之ECB解密------------------");
//        //和加密时密钥一样
//        String aesKey = "ee1b7d9ca0b7d10d2cd27f35d34bb412";
//        //base64编码后的字符串
//        String plainText = "vuQrpYy7xHB63Ha2s8dQdp9GX96XLUJknNebiHzQZpZq9GAe/drIFiCW+TwcCUy93TdN8vMOoGHn7G5jcB1azuNyHjCqR8rYELFR1BvD9j15IR31i3GjSw4U3Y+1LqLohC7q4Gajv4FvC9SNSa4xXpAuV0vARWjZOlSJHYQ9rvG89RK8alYFKrQZDBcMAx+knA7nrJ4LkLpPub6d9rRgon9hK2BVQbVIE1JqJnP6oJGq9gxhEyCg5j/c7PU+gTUvaOlCaByxKjCaVqrCAN1o3SNpjr8HgAIKC7qLpvr6twvnQ2UpIvG81HVeS3w8R/XHS8HO4SX81iPG5G/Y2SV8udCheA8Rh8EXxetibz4USoFRRf6vUebRuHaNhyp9NP5VzJ8+KDh1ySGXoBc1JKKi/s/R2rFzp4mHWtKcL3F0wIgAIJu2KVwI5nzy/zZJLXb4SKLBM5TQrkWt1l1qOeIpGTG6xlgGTjvPTa/+Au+VGYwqN+fUEojyruojlFFwkglYBASLQak0lVJRhOp+9fsrX8oSSHGl0a4NqWNSN1gyASGY1vL3yG0l+vVcjHaj+m9Ac04C0LzdT9zP2Pp1ImpzjTd2xed4X3DCcSJN8DB7JaWLe2iffe807aYz6VVVWDMKTcPpYoIqbX2tHGDO+6AyKHRN6QwBlbyUsSTd30N49GnDTfALcY7mMU374jr8dOfd2ciUDBgAsvFN4vPisn7LH4HH0HQfdiVbnCSZuUA0/NTjq2Jy5WAaY5Kkrh2i5TIg7d6LC1StBI1JPV/9PYGXAIXBSZVR57pz9zstekW2aHKmnlIvwzn2mSvmllH0iboxb0zyoxqZPzMgUO7pS3Ym7g==";
//        System.out.println("ECB解密原始数据：" + plainText);
//        //因为原始字符串经过base64编码后的字符串
//        byte[] key = aesKey.getBytes(StandardCharsets.UTF_8);
//        byte[] data = Base64.getDecoder().decode(plainText);
//        //ECB模式解密
//        byte[] decodeStr = AESUtils.decodeByECB(key, data);
//        System.out.println("ECB解密后转字符串数据：" + new String(decodeStr, StandardCharsets.UTF_8));
//    }
}