package com.jgh.aianalysis.utils.aliyun;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.green20220302.Client;
import com.aliyun.green20220302.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.jgh.aianalysis.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class FileGreenUtil {

    @Value("${aliyun.green.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.green.accessSecret}")
    private String accessSecret;

    @Value("${aliyun.oss.region}")
    private String region;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    public Map fileGreenCheck(String url, String fileType) throws Exception {

        //  ------------------获取任务检测ID-----------------------
        //  1.
        Config config = new Config();
        config.setAccessKeyId(accessKeyId);
        config.setAccessKeySecret(accessSecret);
        // 设置http代理。
        // config.setHttpProxy("http://10.10.xx.xx:xxxx");
        // 设置https代理。
        // config.setHttpsProxy("https://10.10.xx.xx:xxxx");
        // 接入区域和地址请根据实际情况修改
        config.setRegionId("cn-shenzhen");
        config.setEndpoint("green-cip.cn-shenzhen.aliyuncs.com");
        //连接时超时时间，单位毫秒（ms）。
        config.setReadTimeout(6000);
        //读取时超时时间，单位毫秒（ms）。
        config.setConnectTimeout(3000);


        //  2.
        //注意，此处实例化的client请尽可能重复使用，避免重复建立连接，提升检测性能。
        Client client = new Client(config);


        // 检测参数构造。
        Map<String, Object> resultMap = new HashMap<>();
        String file = fileType + url.substring(url.lastIndexOf("/"));

        JSONObject serviceParameters = new JSONObject();
        serviceParameters.put("ossBucketName", bucketName);
        serviceParameters.put("ossObjectName", file);
        serviceParameters.put("ossRegionId", region);

        FileModerationRequest fileModerationRequest = new FileModerationRequest();
        // 检测类型：document_detection 通用文档检测
        fileModerationRequest.setService("document_detection");
        fileModerationRequest.setServiceParameters(serviceParameters.toJSONString());

        String taskId = null;
        try {
            FileModerationResponse response = client.fileModeration(fileModerationRequest);
            // 自动路由。
            if (response != null) {
                //区域切换到cn-beijing。
                if (500 == response.getStatusCode() || (response.getBody() != null && 500 == (response.getBody().getCode()))) {
                    // 接入区域和地址请根据实际情况修改。
                    config.setRegionId("cn-hangzhou");
                    config.setEndpoint("green-cip.cn-hangzhou.aliyuncs.com");
                    client = new Client(config);
                    response = client.fileModeration(fileModerationRequest);
                }
            }
            if (response.getStatusCode() == 200) {
                FileModerationResponseBody result = response.getBody();
                log.info(JSON.toJSONString(result));
                log.info("requestId = " + result.getRequestId());
                log.info("code = " + result.getCode());
                log.info("msg = " + result.getMessage());
                Integer code = result.getCode();
                if (200 == code) {
                    FileModerationResponseBody.FileModerationResponseBodyData data = result.getData();
                    taskId = data.getTaskId();
                    log.info("taskId = [" + taskId + "]");
                } else {
                    log.error("file moderation not success. code:" + code);
                    throw new BusinessException("文件检测失败！！！");
                }
            } else {
                log.error("response not success. status:" + response.getStatusCode());
                throw new BusinessException("文件检测失败！！！");
            }
        } catch (Exception e) {
            log.error("文件检测失败！！！");
            e.printStackTrace();
            throw new BusinessException("文件检测失败！！！");
        }

        if (taskId == null) {
            log.error("文件检测失败！！！");
            throw new BusinessException("文件检测失败！！！");
        }


        //  ------------------------提交任务检测ID获取检测结果-------------------------
        //  将获得的taskId，进行查询任务检测任务获取检测结果。
        JSONObject serviceParameters2 = new JSONObject();
        // 提交任务时返回的taskId
        serviceParameters2.put("taskId", taskId);


        DescribeFileModerationResultRequest describeFileModerationResultRequest = new DescribeFileModerationResultRequest();

        describeFileModerationResultRequest.setService("document_detection");
        describeFileModerationResultRequest.setServiceParameters(serviceParameters2.toJSONString());


        //  3.
        //  3.
        try {
            // 实现轮询机制获取检测结果
            int maxAttempts = 30; // 最大尝试次数
            int attemptInterval = 2000; // 每次尝试间隔2秒

            for (int i = 0; i < maxAttempts; i++) {
                DescribeFileModerationResultResponse response = client.describeFileModerationResult(describeFileModerationResultRequest);
                // 自动路由。
                if (response != null) {
                    //区域切换到cn-beijing。
                    if (500 == response.getStatusCode() || (response.getBody() != null && 500 == (response.getBody().getCode()))) {
                        // 接入区域和地址请根据实际情况修改。
                        config.setRegionId("cn-hangzhou");
                        config.setEndpoint("green-cip.cn-hangzhou.aliyuncs.com");
                        client = new Client(config);
                        response = client.describeFileModerationResult(describeFileModerationResultRequest);
                    }
                }

                if (response.getStatusCode() == 200) {
                    DescribeFileModerationResultResponseBody result = response.getBody();
                    log.info("requestId=" + result.getRequestId());
                    log.info("code=" + result.getCode());
                    log.info("msg=" + result.getMessage());

                    if (200 == result.getCode()) {
                        DescribeFileModerationResultResponseBody.DescribeFileModerationResultResponseBodyData data = result.getData();
                        List<DescribeFileModerationResultResponseBody.DescribeFileModerationResultResponseBodyDataPageResult> pageResult = data.getPageResult();
                        log.info("pageResult = " + JSON.toJSONString(pageResult));
                        log.info("dataId = " + data.getDataId());
                        log.info("url = " + data.getUrl());
                        DescribeFileModerationResultResponseBody.DescribeFileModerationResultResponseBodyDataPageSummary pageSummary = data.getPageSummary();
                        log.info("pageSummary = " + JSON.toJSONString(pageSummary));
                        String riskLevelImage = pageSummary.getImageSummary().getRiskLevel();
                        String riskLevelText = pageSummary.getTextSummary().getRiskLevel();

                        if ("none".equals(riskLevelImage) && "none".equals(riskLevelText)) {
                            // 返回检测结果
                            resultMap.put("suggestion", "pass");
                            return resultMap;
                        }else {
                            resultMap.put("suggestion", "review");
                            return resultMap;
                        }
                    } else if (280 == result.getCode()) {
                        // 280 表示正在处理中，继续轮询
                        log.info("文件检测仍在处理中，等待后重试... (attempt " + (i + 1) + "/" + maxAttempts + ")");
                        if (i < maxAttempts - 1) {
                            Thread.sleep(attemptInterval);
                            continue;
                        }
                    } else {
                        log.error("file moderation result not success. code:" + result.getCode() + ", message: " + result.getMessage());
                        throw new BusinessException("文件检测失败！！！错误码：" + result.getCode() + "，消息：" + result.getMessage());
                    }
                } else {
                    log.error("response not success. status:" + response.getStatusCode());
                    throw new BusinessException("文件检测失败！！！HTTP状态码：" + response.getStatusCode());
                }
            }

            // 如果循环结束仍未成功，则抛出超时异常
            log.error("文件检测超时，未能获取检测结果");
            throw new BusinessException("文件检测超时，请稍后重试！！！");
        } catch (Exception e) {
            log.error("获取文件检测结果失败！！！");
            e.printStackTrace();
            throw new BusinessException("获取文件检测结果失败！！！");
        }

    }
}
