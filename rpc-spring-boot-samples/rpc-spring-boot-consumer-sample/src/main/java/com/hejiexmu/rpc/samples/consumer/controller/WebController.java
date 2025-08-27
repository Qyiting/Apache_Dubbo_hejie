package com.hejiexmu.rpc.samples.consumer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web页面控制器
 * 提供RPC服务调用测试界面的页面路由
 * 
 * @author hejiexmu
 */
@Controller
public class WebController {

    /**
     * 主页 - 重定向到服务调用测试界面
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/rpc-test";
    }

    /**
     * RPC服务调用测试界面
     */
    @GetMapping("/rpc-test")
    public String rpcTest() {
        return "redirect:/index.html";
    }

    /**
     * 服务管理页面
     */
    @GetMapping("/management")
    public String management() {
        return "redirect:/management.html";
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
     * 服务发现页面
     */
    @GetMapping("/management/discovery")
    public String discovery() {
        return "management/discovery";
    }

    /**
     * 调用监控页面
     */
    @GetMapping("/management/metrics")
    public String metrics() {
        return "management/metrics";
    }
}