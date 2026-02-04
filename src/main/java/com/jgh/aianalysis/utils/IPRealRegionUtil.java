package com.jgh.aianalysis.utils;

import cn.hutool.core.io.resource.ClassPathResource;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.InvalidConfigException;
import org.lionsoul.ip2region.service.Ip2Region;
import org.lionsoul.ip2region.xdb.InetAddressException;
import org.lionsoul.ip2region.xdb.XdbException;

import java.io.IOException;

public class IPRealRegionUtil {


    public static String getRegion(String ip) {
// 1, 创建 v4 的配置：指定缓存策略和 v4 的 xdb 文件路径
        final Config v4Config;    // 指定为 v4 配置
        try {
            v4Config = Config.custom()
                    .setCachePolicy(Config.VIndexCache)     // 指定缓存策略:  NoCache / VIndexCache / BufferCache
                    .setSearchers(15)                       // 设置初始化的查询器数量
                    // .setCacheSliceBytes(int)             // 设置缓存的分片字节数，默认为 50MiB
                    // .setXdbInputStream(InputStream)      // 设置 v4 xdb 文件的 inputstream 对象
                    // .setXdbFile(File)                    // 设置 v4 xdb File 对象
                    .setXdbPath("src/main/resources/ipdb/ip2region_v4.xdb")    // 设置 v4 xdb 文件的路径
                    .asV4();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (XdbException e) {
            throw new RuntimeException(e);
        } catch (InvalidConfigException e) {
            throw new RuntimeException(e);
        }

// 2, 创建 v6 的配置：指定缓存策略和 v6 的 xdb 文件路径
        final Config v6Config;    // 指定为 v6 配置
        try {
            v6Config = Config.custom()
                    .setCachePolicy(Config.VIndexCache)     // 指定缓存策略: NoCache / VIndexCache / BufferCache
                    .setSearchers(15)                       // 设置初始化的查询器数量
                    // .setCacheSliceBytes(int)             // 设置缓存的分片字节数，默认为 50MiB
                    // .setXdbInputStream(InputStream)      // 设置 v6 xdb 文件的 inputstream 对象
                    // .setXdbFile(File)                    // 设置 v6 xdb File 对象
                    .setXdbPath("src/main/resources/ipdb/ip2region_v6.xdb")    // 设置 v6 xdb 文件的路径
                    .asV6();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (XdbException e) {
            throw new RuntimeException(e);
        } catch (InvalidConfigException e) {
            throw new RuntimeException(e);
        }

// 备注：Xdb 三种初始化输入的优先级：XdbInputStream -> XdbFile -> XdbPath
// setXdbInputStream 仅方便使用者从 jar 包中加载 xdb 文件内容，这时 cachePolicy 只能设置为 Config.BufferCache

// 3，通过上述配置创建 Ip2Region 查询服务
        final Ip2Region ip2Region;
        try {
            ip2Region = Ip2Region.create(v4Config, v6Config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

// 4，导出 ip2region 服务作为全局变量，进行双版本的IP地址的并发查询，例如：
        String getRegion = null;
        try {
            getRegion = ip2Region.search(ip);                          // 进行 IPv4 查询
        } catch (InetAddressException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

// 5，在服务需要关闭的时候，同时关闭 ip2region 查询服务
// 备注：close 方法只需要在整个服务关闭的时候关闭，查询途中不需要操作
        try {
            ip2Region.close();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return getRegion;
    }
}