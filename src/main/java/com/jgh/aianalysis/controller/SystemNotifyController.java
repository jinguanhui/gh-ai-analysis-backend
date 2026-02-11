package com.jgh.aianalysis.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.modal.dto.OrderPageDto;
import com.jgh.aianalysis.modal.entity.SystemNotification;
import com.jgh.aianalysis.service.SystemNotificationService;
import com.jgh.ghcommon.common.BaseResponse;
import com.jgh.ghcommon.common.PageRequest;
import com.jgh.ghcommon.model.entity.Chart;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/systemNotify")
@Slf4j
public class SystemNotifyController {

    @Resource
    private SystemNotificationService systemNotificationService;

    /**
     * 分页获取系统消息列表
     *
     * @param pageRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<SystemNotification>> getSysNotificationList(@RequestBody PageRequest pageRequest, HttpServletRequest request) {
        log.info("分页获取系统消息列表");

        String header = request.getHeader("userId");
        if (header == null) {
            log.error("用户不存在");
            throw new BusinessException("用户不存在");
        }
        long userId = Long.parseLong(header);

        long current = pageRequest.getCurrent();
        long size = pageRequest.getPageSize();
        QueryWrapper<SystemNotification> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);

        Page<SystemNotification> page = systemNotificationService.page(new Page<>(current, size), queryWrapper);
        return BaseResponse.success(page);

    }

    /**
     * 修改系统消息
     */
    @PostMapping("/update")
    public BaseResponse updateSysNotify(HttpServletRequest request) {
        String header = request.getHeader("userId");
        if (header == null) {
            log.error("用户不存在");
            throw new BusinessException("用户不存在");
        }
        long userId = Long.parseLong(header);

        UpdateWrapper<SystemNotification> wrapper = new UpdateWrapper<>();
        wrapper.eq("userId", userId);
        wrapper.set("isRead", 1);

        boolean update = systemNotificationService.update(wrapper);
        if (!update) {
            log.error("修改系统消息失败");
            throw new BusinessException("修改系统消息失败");
        }

        return BaseResponse.success();
    }


}
