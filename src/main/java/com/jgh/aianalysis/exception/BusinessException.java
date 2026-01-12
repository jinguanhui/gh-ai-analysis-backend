package com.jgh.aianalysis.exception;

import com.jgh.ghcommon.common.ResponseCode;
import lombok.Getter;

/**
 * 自定义异常类
 */
@Getter
public class BusinessException extends RuntimeException {

    private int code;

    private String message;


    public BusinessException(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public BusinessException(ResponseCode responseCode) {
        this.code = responseCode.getCode();
        this.message = responseCode.getMessage();
    }

    public BusinessException(String message) {
        this.code = ResponseCode.ERROR.getCode();
        this.message = message;
    }


}
