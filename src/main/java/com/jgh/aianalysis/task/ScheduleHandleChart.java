package com.jgh.aianalysis.task;

import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.service.AccessKeyService;
import com.jgh.aianalysis.service.ChartService;
import com.jgh.ghcommon.common.ChartStatusEnum;
import com.jgh.ghcommon.model.entity.AccessKey;
import com.jgh.ghcommon.model.entity.Chart;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ScheduleHandleChart {

    @Resource
    private ChartService chartService;

    @Resource
    private AccessKeyService accessKeyService;

    @Scheduled(cron = "* * 0 * * ?")
    public void handleChartError() {
        log.info("开始处理错误图表");
        List<Chart> list = chartService.list();
        List<Chart> chartWithFaileds = list.stream().map(chart -> {
            if (!Objects.equals(chart.getStatus(), ChartStatusEnum.FAILED.getStatus()) &&
                    !Objects.equals(chart.getStatus(), ChartStatusEnum.SUCCEED.getStatus())) {
                chart.setStatus(ChartStatusEnum.FAILED.getStatus());
                return chart;
            }
            return null;
        }).toList();

        boolean b = chartService.updateBatchById(chartWithFaileds);
        if (!b) {
            log.error("定时任务处理错误图表失败");
            throw new BusinessException("定时任务处理错误图表失败");
        }

    }

    @Scheduled(cron = "* * 0 * * ?")
    public void handleKeyError() {
        log.info("开始处理过期秘钥对");
        List<AccessKey> list = accessKeyService.list();
        List<AccessKey> list1 = list.stream().map(accessKey -> {
            if (accessKey.getExpireTime().getTime() >= new Date().getTime()) {
                accessKey.setIsDelete(1);
                return accessKey;
            }
            return null;
        }).toList();

        boolean b = accessKeyService.updateBatchById(list1);
        if (!b) {
            log.error("定时任务删除过期秘钥对失败");
            throw new BusinessException("定时任务删除过期秘钥对失败");
        }

    }

}
