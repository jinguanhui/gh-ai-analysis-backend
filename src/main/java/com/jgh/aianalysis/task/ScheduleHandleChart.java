package com.jgh.aianalysis.task;

import com.jgh.aianalysis.service.ChartService;
import com.jgh.ghcommon.common.ChartStatusEnum;
import com.jgh.ghcommon.model.entity.Chart;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ScheduleHandleChart {

    @Resource
    private ChartService chartService;

    @Scheduled(cron = "* * 0 * * ?")
    public void handleChartError() {
        log.info("开始处理错误图表");
        List<Chart> list = chartService.list();
        List<Chart> chartWithFaileds = list.stream().map(chart -> {
            if (Objects.equals(chart.getStatus(), ChartStatusEnum.FAILED.getStatus())) return chart;
            return null;
        }).toList();

    }

}
