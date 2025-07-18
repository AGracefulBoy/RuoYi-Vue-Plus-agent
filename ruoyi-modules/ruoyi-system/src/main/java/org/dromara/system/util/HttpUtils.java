package org.dromara.system.util;

import cn.hutool.core.io.IoUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * HTTP工具类
 * 提供HTTP请求和流式请求的功能
 *
 * @author ruoyi
 */
@Slf4j
public class HttpUtils {

    /**
     * 发送POST请求
     *
     * @param url     请求URL
     * @param jsonBody JSON请求体
     * @return 响应结果
     */
    public static String sendPost(String url, String jsonBody) {
        try {
            log.info("发送POST请求到: {}", url);
            log.debug("请求体: {}", jsonBody);
            
            HttpResponse response = HttpRequest.post(url)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(jsonBody)
                .timeout(30000)
                .execute();
            
            int status = response.getStatus();
            String result = response.body();
            
            log.info("POST请求响应状态: {}", status);
            log.debug("POST请求响应结果: {}", result);
            
            // 检查HTTP状态码
            if (status >= 400) {
                String errorMsg = String.format("HTTP请求失败，状态码: %d, 响应: %s", status, result);
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
            return result;
        } catch (Exception e) {
            log.error("POST请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("HTTP请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送POST流式请求
     *
     * @param url          请求URL
     * @param jsonBody     JSON请求体
     * @param outputStream 输出流
     * @return 是否成功
     */
    public static boolean sendPostStream(String url, String jsonBody, OutputStream outputStream) {
        HttpURLConnection connection = null;
        try {
            log.info("发送POST流式请求到: {}", url);
            log.debug("请求体: {}", jsonBody);
            
            URL urlObj = new URL(url);
            connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);
            
            // 发送请求体
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            
            // 获取响应状态码
            int responseCode = connection.getResponseCode();
            log.info("POST流式请求响应状态: {}", responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 流式读取响应
                try (InputStream inputStream = connection.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    
                    char[] buffer = new char[1024];
                    int bytesRead;
                    while ((bytesRead = reader.read(buffer)) != -1) {
                        String chunk = new String(buffer, 0, bytesRead);
                        outputStream.write(chunk.getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    }
                }
                return true;
            } else {
                // 读取错误信息
                try (InputStream errorStream = connection.getErrorStream()) {
                    if (errorStream != null) {
                        String errorMsg = IoUtil.read(errorStream, StandardCharsets.UTF_8);
                        log.error("POST流式请求失败，错误信息: {}", errorMsg);
                        outputStream.write(("错误: " + errorMsg).getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    }
                }
                return false;
            }
        } catch (Exception e) {
            log.error("POST流式请求失败: {}", e.getMessage(), e);
            try {
                outputStream.write(("错误: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (IOException ioException) {
                log.error("写入错误信息到输出流失败: {}", ioException.getMessage());
            }
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}