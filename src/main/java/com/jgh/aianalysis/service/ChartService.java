package com.jgh.aianalysis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jgh.ghcommon.common.BaseResponse;
import com.jgh.ghcommon.model.dto.chart.GenChartByAiRequest;
import com.jgh.ghcommon.model.entity.Chart;
import com.jgh.ghcommon.model.vo.BiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author 15180
 * @description 针对表【chart(图表信息表)】的数据库操作Service
 * @createDate 2025-12-30 15:27:39
 */
public interface ChartService extends IService<Chart> {

    /**
     * 生成图表
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    BaseResponse<BiResponse> genChartByAi(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request);

    /**
     * 生成图表（同步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    BaseResponse<BiResponse> genChartByAiSync(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request);
}
