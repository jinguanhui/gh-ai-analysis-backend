package com.jgh.aianalysis.service;

import com.jgh.aianalysis.modal.dto.OrderPayDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;

public interface AlipayService {
    String payWithCode(OrderPayDto orderPayDto, HttpServletRequest request, HttpServletResponse response);
}
