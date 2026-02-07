package com.jgh.aianalysis.config;

import com.alipay.api.AlipayConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AliPayConfig {

    @Bean
    public AlipayConfig alipayConfig(AliPayConfigInfo aliPayConfigInfo) {
        AlipayConfig alipayConfig = new AlipayConfig();
        alipayConfig.setServerUrl(aliPayConfigInfo.getGatewayHost());
        alipayConfig.setSignType(aliPayConfigInfo.getSignType());
        alipayConfig.setAppId(aliPayConfigInfo.getAppId());
        alipayConfig.setFormat(alipayConfig.getFormat());
        //设置字符集
        alipayConfig.setCharset(aliPayConfigInfo.getCharset());
        // 为避免私钥随源码泄露，推荐从文件中读取私钥字符串而不是写入源码中
        alipayConfig.setPrivateKey(aliPayConfigInfo.getMerchantPrivateKey());
        //注：如果采用非证书模式，则无需赋值上面的三个证书路径，改为赋值如下的支付宝公钥字符串即可
        alipayConfig.setAlipayPublicKey(aliPayConfigInfo.getAlipayPublicKey());
        return alipayConfig;
    }
}