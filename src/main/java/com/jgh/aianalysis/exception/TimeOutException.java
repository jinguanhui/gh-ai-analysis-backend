package com.jgh.aianalysis.exception;

import com.jgh.ghcommon.common.ResponseCode;
import lombok.Getter;

/**
 * 自定义异常类
 */
@Getter
public class TimeOutException extends RuntimeException {

    private int code;

    private String message;


    public TimeOutException(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public TimeOutException(ResponseCode responseCode) {
        this.code = responseCode.getCode();
        this.message = responseCode.getMessage();
    }

    public TimeOutException(String message) {
        this.code = ResponseCode.ERROR.getCode();
        this.message = message;
    }


}
