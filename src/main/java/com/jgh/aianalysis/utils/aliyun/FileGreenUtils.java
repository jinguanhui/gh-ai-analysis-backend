package com.jgh.aianalysis.utils.aliyun;

import com.alibaba.fastjson.JSON;
import com.aliyun.green20220302.Client;
import com.aliyun.green20220302.models.ImageModerationRequest;
import com.aliyun.green20220302.models.ImageModerationResponse;
import com.aliyun.green20220302.models.ImageModerationResponseBody;
import com.aliyun.green20220302.models.ImageModerationResponseBody.ImageModerationResponseBodyData;
import com.aliyun.green20220302.models.ImageModerationResponseBody.ImageModerationResponseBodyDataResult;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.jgh.aianalysis.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class FileGreenUtils {

    @Value("${aliyun.green.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.green.accessSecret}")
    private String accessSecret;

    @Value("${aliyun.oss.region}")
    private String region;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    public Map fileGreenCheck(String url) throws Exception {
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


        //  2.
        //注意，此处实例化的client请尽可能重复使用，避免重复建立连接，提升检测性能。
        Client client = new Client(config);

        // 创建RuntimeObject实例并设置运行参数
        RuntimeOptions runtime = new RuntimeOptions();

        // 检测参数构造。
        //检测参数构造
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, String> serviceParameters = new HashMap<>();

        //  https://ai-analysis-jgh.oss-cn-shenzhen.aliyuncs.com/68695d5b-416e-4447-b52f-bc15887e54ff.jpg
        //  将上面的URL地址转化为类似这种的文件名image/001.jpg
        String file ="image"+ url.substring(url.lastIndexOf("/"));
        log.info("阿里云OSS文件路径为：{}",  file);
        //待检测数据唯一标识
        serviceParameters.put("dataId", UUID.randomUUID().toString());
        // 待检测文件所在bucket的区域。 示例：cn-shanghai
        serviceParameters.put("ossRegionId", region);
        // 待检测文件所在bucket名称。示例：bucket001
        serviceParameters.put("ossBucketName", bucketName);
        // 待检测文件。 示例：image/001.jpg
        serviceParameters.put("ossObjectName", file);

        ImageModerationRequest request = new ImageModerationRequest();
        // 图片检测service：内容安全控制台图片增强版规则配置的serviceCode，示例：baselineCheck
        // 支持service请参考：https://help.aliyun.com/document_detail/467826.html?0#p-23b-o19-gff
        request.setService("baselineCheck");
        request.setServiceParameters(JSON.toJSONString(serviceParameters));

        ImageModerationResponse response = null;
        try {
            response = client.imageModerationWithOptions(request, runtime);
        } catch (Exception e) {
            log.error("图片检测失败！！！");
            e.printStackTrace();
            throw new BusinessException("图片检测失败！！！");
        }


        //  3.
        try {
            // 自动路由。
            if (response != null) {
                //区域切换到cn-beijing。
                if (500 == response.getStatusCode() || (response.getBody() != null && 500 == (response.getBody().getCode()))) {
                    // 接入区域和地址请根据实际情况修改。
                    config.setRegionId("cn-hangzhou");
                    config.setEndpoint("green-cip.cn-hangzhou.aliyuncs.com");
                    client = new Client(config);
                    response = client.imageModerationWithOptions(request, runtime);
                }
            }
            // 打印检测结果。
            if (response != null) {
                if (response.getStatusCode() == 200) {
                    ImageModerationResponseBody body = response.getBody();
                    log.info("requestId=" + body.getRequestId());
                    log.info("code=" + body.getCode());
                    log.info("msg=" + body.getMsg());
                    if (body.getCode() == 200) {
                        ImageModerationResponseBodyData data = body.getData();
                        log.info("dataId=" + data.getDataId());
                        List<ImageModerationResponseBodyDataResult> results = data.getResult();
                        for (ImageModerationResponseBodyDataResult result : results) {
                            log.info("label=" + result.getLabel());
                            log.info("confidence=" + result.getConfidence());
                            if (!"nonLabel".equals(result.getLabel()) && result.getConfidence() != null) {
                                resultMap.put("suggestion","review");
                                return resultMap;
                            }
                        }
                        resultMap.put("suggestion","pass");
                        return resultMap;
                    } else {
                        log.error("图片检测失败！！！");
                        log.error("image moderation not success. code:" + body.getCode());
                        throw new BusinessException("图片检测失败！！！");
                    }
                } else {
                    log.error("图片检测失败！！！");
                    log.error("response not success. status:" + response.getStatusCode());
                    throw new BusinessException("图片检测失败！！！");
                }
            }
        } catch (Exception e) {
            log.error("图片检测失败！！！");
            e.printStackTrace();
            throw new BusinessException("图片检测失败！！！");
        }


        return null;
    }

}
