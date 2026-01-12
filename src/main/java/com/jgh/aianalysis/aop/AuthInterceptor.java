package com.jgh.aianalysis.aop;

import com.jgh.aianalysis.annotation.AuthCheck;
import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.service.UserService;
import com.jgh.ghcommon.common.ResponseCode;
import com.jgh.ghcommon.constant.UserConstant;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


/**
 * 权限校验 AOP
 *
 * @author jgh
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行拦截
     *
     * @param joinPoint
     * @param authCheck
     * @return
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 当前登录用户角色
        String userRole = request.getHeader("userRole");
        // 必须有管理员权限
        if (!String.valueOf(UserConstant.ADMIN_ROLE).equals(userRole)) {
            throw new BusinessException(ResponseCode.NOT_AUTH);
        }
        // 通过权限校验，放行
        return joinPoint.proceed();
    }
}

