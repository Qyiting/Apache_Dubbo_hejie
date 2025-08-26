package com.hejiexmu.rpc.samples.provider.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web页面控制器
 * 提供管理界面的页面路由
 * 
 * @author hejiexmu
 */
@Controller
public class WebController {

    /**
     * 主页 - 重定向到管理界面
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/management";
    }

    /**
     * 管理界面主页
     */
    @GetMapping("/management")
    public String management() {
        return "redirect:/index.html";
    }

    /**
     * 服务概览页面
     */
    @GetMapping("/management/overview")
    public String overview() {
        return "management/overview";
    }

    /**
     * 健康监控页面
     */
    @GetMapping("/management/health")
    public String health() {
        return "management/health";
    }

    /**
     * 配置管理页面
     */
    @GetMapping("/management/config")
    public String config() {
        return "management/config";
    }

    /**
     * 日志查看页面
     */
    @GetMapping("/management/logs")
    public String logs() {
        return "management/logs";
    }

    /**
     * 性能指标页面
     */
    @GetMapping("/management/metrics")
    public String metricsPage() {
        return "management/metrics";
    }
    
    @GetMapping("/management/instances")
    public String instancesPage() {
        return "management/instances";
    }
}