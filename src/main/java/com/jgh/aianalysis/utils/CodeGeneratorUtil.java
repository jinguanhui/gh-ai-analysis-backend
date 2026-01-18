package com.jgh.aianalysis.utils;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;

/**
 * @author young
 * @date 2022/10/18 16:30
 * @description: 生成验证码
 */
public class CodeGeneratorUtil {
    /**
     * 生成指定长度的验证码
     * @param length 长度
     * @return
     */
    public static String generateCode(int length){
       return RandomUtil.randomNumbers(6);
    }
}