package com.jgh.aianalysis.utils.aliyun;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.green20220302.Client;
import com.aliyun.green20220302.models.TextModerationPlusRequest;
import com.aliyun.green20220302.models.TextModerationPlusResponse;
import com.aliyun.green20220302.models.TextModerationPlusResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.jgh.aianalysis.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class TextGreenUtils {

    @Value("${aliyun.green.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.green.accessSecret}")
    private String accessSecret;

    public Map greenTextScanPlusVersion(String content) throws Exception {
        Config config = new Config();
        config.setAccessKeyId(accessKeyId);
        config.setAccessKeySecret(accessSecret);
        //接入区域和地址请根据实际情况修改
        config.setRegionId("cn-shenzhen");
        config.setEndpoint("green-cip.cn-shenzhen.aliyuncs.com");
        //读取时超时时间，单位毫秒（ms）。
        config.setReadTimeout(6000);
        //连接时超时时间，单位毫秒（ms）。
        config.setConnectTimeout(3000);
        Client client = new Client(config);

        // 创建RuntimeObject实例并设置运行参数。
        RuntimeOptions runtime = new RuntimeOptions();
        runtime.readTimeout = 10000;
        runtime.connectTimeout = 10000;


        //检测参数构造
        Map<String, Object> resultMap = new HashMap<>();
        JSONObject serviceParameters = new JSONObject();
        serviceParameters.put("content", content);

        if (serviceParameters.get("content") == null || serviceParameters.getString("content").trim().isEmpty()) {
            log.error("检测内容为空");
            resultMap.put("suggestion", "检测内容为空");
            return resultMap;
        }

        TextModerationPlusRequest textModerationPlusRequest = new TextModerationPlusRequest();
        // 检测类型
        textModerationPlusRequest.setService("comment_detection_pro");
        textModerationPlusRequest.setServiceParameters(serviceParameters.toJSONString());

        try {
            TextModerationPlusResponse response = client.textModerationPlus(textModerationPlusRequest);
            // 自动路由。
            if (response != null) {
                // 服务端错误，区域切换到cn-hangzhou。
                if (500 == response.getStatusCode() || (response.getBody() != null && 500 == (response.getBody().getCode()))) {
                    // 接入区域和地址请根据实际情况修改。
                    config.setRegionId("cn-hangzhou");
                    config.setEndpoint("green-cip.cn-hangzhou.aliyuncs.com");
                    client = new Client(config);
                    response = client.textModerationPlus(textModerationPlusRequest);
                }

            }

            if (response != null) {
                if (response.getStatusCode() == 200) {
                    TextModerationPlusResponseBody result = response.getBody();
                    log.info("阿里云内容检查响应为：");
                    log.info(JSON.toJSONString(result));
                    log.info("requestId = " + result.getRequestId());
                    log.info("code = " + result.getCode());
                    log.info("msg = " + result.getMessage());
                    Integer code = result.getCode();
                    if (200 == code) {
                        TextModerationPlusResponseBody.TextModerationPlusResponseBodyData data = result.getData();
                        log.info(JSON.toJSONString(data, true));
                        log.info("风险等级为：{}", data.getRiskLevel());
                        if ("none".equals(data.getRiskLevel())) {
                            log.info("审核通过");
                            resultMap.put("suggestion", "pass");
                        }else {
                            log.error("文本内容违规");
                            resultMap.put("suggestion", "review");
                        }
                        return resultMap;
                    } else {
                        resultMap.put("suggestion", "review");
                        log.error("text moderation not success. code:" + code);
                    }
                } else {
                    log.error("response not success. status:" + response.getStatusCode());
                    resultMap.put("suggestion", "review");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("文本内容检测异常!!!");
        }

        return null;
    }

}
