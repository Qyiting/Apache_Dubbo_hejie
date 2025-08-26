package com.hejiexmu.rpc.samples.provider.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.util.*;

/**
 * 远程实例服务
 * 用于与其他RPC实例进行HTTP通信
 */
@Slf4j
@Service
public class RemoteInstanceService {
    
    private final RestTemplate restTemplate;
    
    public RemoteInstanceService() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * 获取远程实例配置
     */
    public Map<String, Object> getRemoteInstanceConfig(String instanceId) {
        try {
            String[] parts = instanceId.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid instanceId format: " + instanceId);
            }
            
            String host = parts[0];
            String rpcPort = parts[1];
            int webPort = Integer.parseInt(rpcPort) - 1000;
            
            String url = String.format("http://%s:%d/api/management/config", host, webPort);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Cache-Control", "no-cache");
            headers.set("Pragma", "no-cache");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, 
                new ParameterizedTypeReference<Map<String, Object>>() {});
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to get config, status: " + response.getStatusCode());
            }
            
        } catch (ResourceAccessException e) {
            log.warn("无法连接到远程实例 {}: {}", instanceId, e.getMessage());
            throw new RuntimeException("远程实例不可达: " + e.getMessage());
        } catch (Exception e) {
            log.error("获取远程实例配置失败: {}", instanceId, e);
            throw new RuntimeException("获取远程实例配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取远程实例详情
     */
    public Map<String, Object> getRemoteInstanceDetail(String instanceId) {
        try {
            String[] parts = instanceId.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid instanceId format: " + instanceId);
            }
            
            String host = parts[0];
            String rpcPort = parts[1];
            int webPort = Integer.parseInt(rpcPort) - 1000;
            
            String url = String.format("http://%s:%d/api/management/instances/current-instance", host, webPort);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Cache-Control", "no-cache");
            headers.set("Pragma", "no-cache");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, 
                new ParameterizedTypeReference<Map<String, Object>>() {});
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to get instance detail, status: " + response.getStatusCode());
            }
            
        } catch (ResourceAccessException e) {
            log.warn("无法连接到远程实例 {}: {}", instanceId, e.getMessage());
            throw new RuntimeException("远程实例不可达: " + e.getMessage());
        } catch (Exception e) {
            log.error("获取远程实例详情失败: {}", instanceId, e);
            throw new RuntimeException("获取远程实例详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取远程实例日志
     */
    public List<Map<String, Object>> getRemoteInstanceLogs(String instanceId, int lines, String level) {
        try {
            String[] parts = instanceId.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid instanceId format: " + instanceId);
            }
            
            String host = parts[0];
            String rpcPort = parts[1];
            int webPort = Integer.parseInt(rpcPort) - 1000;
            
            String url = String.format("http://%s:%d/api/management/logs?lines=%d&level=%s", 
                host, webPort, lines, level);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Cache-Control", "no-cache");
            headers.set("Pragma", "no-cache");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, 
                new ParameterizedTypeReference<Map<String, Object>>() {});
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && responseBody.containsKey("logs")) {
                    return (List<Map<String, Object>>) responseBody.get("logs");
                }
            }
            
            throw new RuntimeException("Failed to get logs, status: " + response.getStatusCode());
            
        } catch (ResourceAccessException e) {
            log.warn("无法连接到远程实例 {}: {}", instanceId, e.getMessage());
            throw new RuntimeException("远程实例不可达: " + e.getMessage());
        } catch (Exception e) {
            log.error("获取远程实例日志失败: {}", instanceId, e);
            throw new RuntimeException("获取远程实例日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 导出远程实例日志
     */
    public String exportRemoteInstanceLogs(String instanceId) {
        try {
            String[] parts = instanceId.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid instanceId format: " + instanceId);
            }
            
            String host = parts[0];
            String rpcPort = parts[1];
            int webPort = Integer.parseInt(rpcPort) - 1000;
            
            String url = String.format("http://%s:%d/api/management/instances/current-instance/logs/export", host, webPort);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Cache-Control", "no-cache");
            headers.set("Pragma", "no-cache");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<byte[]> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, byte[].class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return new String(response.getBody());
            } else {
                throw new RuntimeException("Failed to export logs, status: " + response.getStatusCode());
            }
            
        } catch (ResourceAccessException e) {
            log.warn("无法连接到远程实例 {}: {}", instanceId, e.getMessage());
            throw new RuntimeException("远程实例不可达: " + e.getMessage());
        } catch (Exception e) {
            log.error("导出远程实例日志失败: {}", instanceId, e);
            throw new RuntimeException("导出远程实例日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查远程实例是否可达
     */
    public boolean isRemoteInstanceReachable(String instanceId) {
        try {
            String[] parts = instanceId.split(":");
            if (parts.length != 2) {
                return false;
            }
            
            String host = parts[0];
            String rpcPort = parts[1];
            int webPort = Integer.parseInt(rpcPort) - 1000;
            
            String url = String.format("http://%s:%d/api/management/health", host, webPort);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Cache-Control", "no-cache");
            headers.set("Pragma", "no-cache");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, 
                new ParameterizedTypeReference<Map<String, Object>>() {});
            
            return response.getStatusCode() == HttpStatus.OK;
            
        } catch (Exception e) {
            log.debug("远程实例 {} 不可达: {}", instanceId, e.getMessage());
            return false;
        }
    }
}