package com.jgh.aianalysis.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.green20220302.Client;
import com.aliyun.green20220302.models.FileModerationRequest;
import com.aliyun.green20220302.models.FileModerationResponse;
import com.aliyun.green20220302.models.FileModerationResponseBody;
import com.aliyun.teaopenapi.models.Config;

import java.util.Map;

public class FileGreenUtils {

    public Map a() throws Exception {
        Config config = new Config();
        /**
         * 阿里云账号AccessKey拥有所有API的访问权限，建议您使用RAM用户进行API访问或日常运维。
         * 常见获取环境变量方式：
         * 方式一：
         *     获取RAM用户AccessKey ID：System.getenv("ALIBABA_CLOUD_ACCESS_KEY_ID");
         *     获取RAM用户AccessKey Secret：System.getenv("ALIBABA_CLOUD_ACCESS_KEY_SECRET");
         * 方式二：
         *     获取RAM用户AccessKey ID：System.getProperty("ALIBABA_CLOUD_ACCESS_KEY_ID");
         *     获取RAM用户AccessKey Secret：System.getProperty("ALIBABA_CLOUD_ACCESS_KEY_SECRET");
         */
        config.setAccessKeyId("建议从环境变量中获取RAM用户AccessKey ID");
        config.setAccessKeySecret("建议从环境变量中获取RAM用户AccessKey Secret");
        //接入区域和地址请根据实际情况修改。
        config.setRegionId("cn-shanghai");
        config.setEndpoint("green-cip.cn-shanghai.aliyuncs.com");
        //连接时超时时间，单位毫秒（ms）。
        config.setReadTimeout(6000);
        //读取时超时时间，单位毫秒（ms）。
        config.setConnectTimeout(3000);

        Client client = new Client(config);
        JSONObject serviceParameters = new JSONObject();
        serviceParameters.put("url", "https://xxx.oss.aliyuncs.com/xxx.pdf"); // 文件URL

        FileModerationRequest fileModerationRequest = new FileModerationRequest();
        // 检测类型：document_detection 通用文档检测
        fileModerationRequest.setService("document_detection");
        fileModerationRequest.setServiceParameters(serviceParameters.toJSONString());

        try {
            FileModerationResponse response = client.fileModeration(fileModerationRequest);
            if (response.getStatusCode() == 200) {
                FileModerationResponseBody result = response.getBody();
                System.out.println(JSON.toJSONString(result));
                System.out.println("requestId = " + result.getRequestId());
                System.out.println("code = " + result.getCode());
                System.out.println("msg = " + result.getMessage());
                Integer code = result.getCode();
                if (200 == code) {
                    FileModerationResponseBody.FileModerationResponseBodyData data = result.getData();
                    System.out.println("taskId = [" + data.getTaskId() + "]");
                } else {
                    System.out.println("file moderation not success. code:" + code);
                }
            } else {
                System.out.println("response not success. status:" + response.getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
