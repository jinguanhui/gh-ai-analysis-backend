package com.jgh.aianalysis.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jgh.aianalysis.annotation.AuthCheck;
import com.jgh.aianalysis.exception.BusinessException;
import com.jgh.aianalysis.manager.SseEmitterManager;
import com.jgh.aianalysis.service.ChartService;
import com.jgh.aianalysis.service.UserService;
import com.jgh.aianalysis.utils.SqlUtils;
import com.jgh.ghcommon.common.BaseResponse;
import com.jgh.ghcommon.common.ResponseCode;
import com.jgh.ghcommon.constant.CommonConstant;
import com.jgh.ghcommon.constant.UserConstant;
import com.jgh.ghcommon.model.dto.chart.*;
import com.jgh.ghcommon.model.entity.Chart;
import com.jgh.ghcommon.model.vo.BiResponse;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


/**
 * 帖子接口
 *
 * @author jgh
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private SseEmitterManager sseEmitterManager;

    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        log.info("添加图表信息！", chartAddRequest);
        if (chartAddRequest == null) {
            throw new BusinessException(ResponseCode.PARAM_NULL);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        chart.setUserId(Long.valueOf(request.getHeader("userId")));
        boolean result = chartService.save(chart);
        if (!result) {
            throw new BusinessException("数据库操作错误！添加图表失败");
        }
        long newChartId = chart.getId();
        return BaseResponse.success(newChartId);
    }

    /**
     * 删除
     *
     * @param id
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody Long id, HttpServletRequest request) {
        log.info("删除图表!");
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_NULL);
        }

        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        if (oldChart == null) {
            throw new BusinessException("图表不存在！");
        }
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(Long.valueOf(request.getHeader("userId"))) && !isAdmin(request)) {
            throw new BusinessException("不是本人操作或管理员操作！");
        }
        boolean b = chartService.removeById(id);
        return BaseResponse.success(b);
    }

    /**
     * 智能分析
     * @param multipartFile
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByAi(
            @RequestParam("file") MultipartFile multipartFile,
            HttpServletRequest request) {

        log.info("智能分析controller！");

        /// 从RequestContextHolder获取当前请求，这应该是拦截器中设置的包装请求
        org.springframework.web.context.request.RequestAttributes requestAttributes =
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();

        HttpServletRequest currentRequest = null;
        if (requestAttributes instanceof org.springframework.web.context.request.ServletRequestAttributes) {
            currentRequest = ((org.springframework.web.context.request.ServletRequestAttributes) requestAttributes).getRequest();
        }

        String name = currentRequest.getParameter("name");
        String goal = currentRequest.getParameter("goal");
        String chartType = currentRequest.getParameter("chartType");

        if (StringUtils.isAnyBlank(name, goal, chartType)) {
            throw new BusinessException("参数为空");
        }

        GenChartByAiRequest genChartByAiRequest = new GenChartByAiRequest();
        genChartByAiRequest.setName(name);
        genChartByAiRequest.setGoal(goal);
        genChartByAiRequest.setChartType(chartType);


        return chartService.genChartByAi(multipartFile, genChartByAiRequest, request);
    }

    /**
     * 第二步：建立SSE连接，监听进度
     */
    @GetMapping("/progress/{taskId}")
    public SseEmitter listenProgress(@PathVariable String taskId) {
        // 创建SSE连接并缓存
        return sseEmitterManager.getEmitter(taskId);
    }


    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck()
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        // 参数校验
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        if (oldChart == null) {
            throw new BusinessException("当前图表不存在！");
        }
        boolean result = chartService.updateById(chart);
        return BaseResponse.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException("图表数据为空！");
        }
        return BaseResponse.success(chart);
    }

    /**
     * 分页获取列表（仅管理员）
     *
     * @param chartQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck()
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return BaseResponse.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        log.info("获取当前用户创建的图表！", chartQueryRequest);
        if (chartQueryRequest == null) {
            throw new BusinessException(ResponseCode.PARAM_NULL);
        }
        Long userId = Long.valueOf(request.getHeader("userId"));
        chartQueryRequest.setUserId(userId);
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        log.info("chartPage.getRecords()");
        return BaseResponse.success(chartPage);
    }

    // endregion

    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        // 参数校验
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        if (oldChart == null) {
            throw new BusinessException("图表数据不存在");
        }
        String userId = request.getHeader("userId");
        String oldUserId = String.valueOf(oldChart.getUserId());
        // 仅本人或管理员可编辑
        if (!oldUserId.equals(userId) && !isAdmin(request)) {
            throw new BusinessException("不是本人或者管理员操作！");
        }
        boolean result = chartService.updateById(chart);
        return BaseResponse.success(result);
    }

    private boolean isAdmin(HttpServletRequest request) {

        String userRole = request.getHeader("userRole");

        return userRole != null && Objects.equals(userRole, String.valueOf(UserConstant.ADMIN_ROLE));
    }

    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();
        Date beginTime = chartQueryRequest.getBeginTime();
        Date endTime = chartQueryRequest.getEndTime();

        // 只有当id大于0时才添加id查询条件
        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.like(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        //  根据时间范围查询--beginTime, endTime
        queryWrapper.between(ObjectUtils.isNotEmpty(beginTime) && ObjectUtils.isNotEmpty(endTime), "createTime", beginTime, endTime);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

}
