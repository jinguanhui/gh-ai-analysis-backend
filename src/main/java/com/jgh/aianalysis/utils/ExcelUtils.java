package com.jgh.aianalysis.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Excel 相关工具类
 */
@Slf4j
public class ExcelUtils {

    /**
     * excel 转 csv
     *
     * @param multipartFile
     * @return
     */
    @SneakyThrows
    public static String excelToCsv(MultipartFile multipartFile) {
        // 检查文件是否存在
        if (multipartFile.isEmpty() || multipartFile.getInputStream() == null) {
            throw new IllegalArgumentException("上传的文件为空或不存在");
        }

        // 读取数据
        List<Map<Integer, String>> list = null;
        try {
            list = EasyExcel.read(multipartFile.getInputStream())
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (IOException e) {
            log.error("表格处理错误", e);
            throw e;
        }

        // 如果数据为空
        if (CollectionUtils.isEmpty(list)) {
            return "";
        }

        // 转换为 csv
        StringBuilder stringBuilder = new StringBuilder();
        // 读取表头(第一行)
        LinkedHashMap<Integer, String> headerMap = (LinkedHashMap) list.get(0);
        List<String> headerList = headerMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
        stringBuilder.append(StringUtils.join(headerList, ",")).append("\n");
        // 读取数据(读取完表头之后，从第一行开始读取)
        for (int i = 1; i < list.size(); i++) {
            LinkedHashMap<Integer, String> dataMap = (LinkedHashMap) list.get(i);
            List<String> dataList = dataMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
            stringBuilder.append(StringUtils.join(dataList, ",")).append("\n");
        }
        return stringBuilder.toString();
    }

    /**
     * 从字节数组转换Excel为CSV
     *
     * @param fileBytes 文件字节数组
     * @return CSV格式字符串
     */
    @SneakyThrows
    public static String excelToCsvFromBytes(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("文件字节数组为空");
        }

        Path tempFile = null;
        try {
            // 创建临时文件
            tempFile = Files.createTempFile("excel_temp_", ".xlsx");
            // 将字节数组写入临时文件
            Files.write(tempFile, fileBytes);

            // 读取临时文件并转换为CSV
            return excelToCsvFromFile(tempFile.toFile());
        } finally {
            // 确保临时文件被删除
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("临时文件删除失败: {}", tempFile.toString(), e);
                }
            }
        }
    }

    /**
     * 从File对象转换Excel为CSV
     *
     * @param file Excel文件
     * @return CSV格式字符串
     */
    @SneakyThrows
    public static String excelToCsvFromFile(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("Excel文件不存在");
        }

        // 读取数据
        List<Map<Integer, String>> list = null;
        try (FileInputStream fis = new FileInputStream(file)) {
            list = EasyExcel.read(fis)
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (IOException e) {
            log.error("表格处理错误", e);
            throw e;
        }

        // 如果数据为空
        if (CollectionUtils.isEmpty(list)) {
            return "";
        }

        // 转换为 csv
        StringBuilder stringBuilder = new StringBuilder();
        // 读取表头(第一行)
        LinkedHashMap<Integer, String> headerMap = (LinkedHashMap) list.get(0);
        List<String> headerList = headerMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
        stringBuilder.append(StringUtils.join(headerList, ",")).append("\n");
        // 读取数据(读取完表头之后，从第一行开始读取)
        for (int i = 1; i < list.size(); i++) {
            LinkedHashMap<Integer, String> dataMap = (LinkedHashMap) list.get(i);
            List<String> dataList = dataMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
            stringBuilder.append(StringUtils.join(dataList, ",")).append("\n");
        }
        return stringBuilder.toString();
    }
}
