package com.jgh.aianalysis.service;

import com.jgh.ghcommon.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface SmsService {

    Boolean sendLoginCode(String phone);

    User verifyCode(String phone, String code, HttpServletRequest request, HttpServletResponse response);


    User verify(String email, String code, HttpServletRequest request, HttpServletResponse response);
}
