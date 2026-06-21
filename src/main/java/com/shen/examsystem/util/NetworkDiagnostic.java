package com.shen.examsystem.util;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.IOException;

/**
 * 网络诊断工具
 * 用于排查AI API连接问题
 */
@Slf4j
public class NetworkDiagnostic {

    /**
     * 诊断AI API连接
     */
    public static String diagnose(String apiUrl) {
        StringBuilder result = new StringBuilder();
        result.append("========== 网络诊断报告 ==========\n");
        
        // 1. 检查DNS解析
        result.append("\n[1] DNS解析检查:\n");
        try {
            URL url = new URL(apiUrl);
            String host = url.getHost();
            InetAddress address = InetAddress.getByName(host);
            result.append("  ✓ 域名: ").append(host).append("\n");
            result.append("  ✓ IP地址: ").append(address.getHostAddress()).append("\n");
        } catch (Exception e) {
            result.append("  ✗ DNS解析失败: ").append(e.getMessage()).append("\n");
            result.append("  原因: 无法解析API服务器域名，请检查网络DNS设置\n");
        }
        
        // 2. 检查HTTP连接
        result.append("\n[2] HTTP连接检查:\n");
        HttpURLConnection connection = null;
        try {
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000); // 10秒
            connection.setReadTimeout(10000);
            connection.setRequestMethod("GET");
            
            int responseCode = connection.getResponseCode();
            result.append("  ✓ 连接成功\n");
            result.append("  ✓ 响应码: ").append(responseCode).append("\n");
        } catch (java.net.ConnectException e) {
            result.append("  ✗ 连接被拒绝: ").append(e.getMessage()).append("\n");
            result.append("  原因: 服务器拒绝连接，可能是端口被阻止或服务未启动\n");
        } catch (java.net.SocketTimeoutException e) {
            result.append("  ✗ 连接超时: ").append(e.getMessage()).append("\n");
            result.append("  原因: 网络延迟过高或服务器无响应\n");
        } catch (java.net.UnknownHostException e) {
            result.append("  ✗ 未知主机: ").append(e.getMessage()).append("\n");
            result.append("  原因: DNS无法解析域名\n");
        } catch (IOException e) {
            result.append("  ✗ IO异常: ").append(e.getMessage()).append("\n");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        
        // 3. 检查常见问题
        result.append("\n[3] 常见问题排查:\n");
        result.append("  • 检查是否使用VPN或代理\n");
        result.append("  • 检查防火墙设置\n");
        result.append("  • 检查网络是否需要认证（如校园网）\n");
        result.append("  • 尝试使用手机热点测试\n");
        
        result.append("\n==========================================\n");
        
        String report = result.toString();
        log.info(report);
        return report;
    }
}
