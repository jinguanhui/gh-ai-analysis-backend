package com.jgh.aianalysis.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AlipayService {
    String payWithCode(HttpServletRequest request, HttpServletResponse response);
}
