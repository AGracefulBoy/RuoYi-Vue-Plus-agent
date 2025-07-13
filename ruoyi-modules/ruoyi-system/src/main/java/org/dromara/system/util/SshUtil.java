package org.dromara.system.util;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.system.config.PythonProperties;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * SSH连接工具类
 *
 * @author ruoyi
 */
@Slf4j
public class SshUtil {

    /**
     * 执行远程SSH命令
     *
     * @param remoteConfig 远程配置
     * @param command      要执行的命令
     * @return 命令执行结果
     */
    public static String executeRemoteCommand(PythonProperties.Remote remoteConfig, String command) {
        Session session = null;
        Channel channel = null;
        try {
            // 创建JSch对象
            JSch jsch = new JSch();

            // 设置私钥
            if (remoteConfig.getPrivateKeyPath() != null && !remoteConfig.getPrivateKeyPath().trim().isEmpty()) {
                if (remoteConfig.getPrivateKeyPassphrase() != null && !remoteConfig.getPrivateKeyPassphrase().trim().isEmpty()) {
                    jsch.addIdentity(remoteConfig.getPrivateKeyPath(), remoteConfig.getPrivateKeyPassphrase());
                } else {
                    jsch.addIdentity(remoteConfig.getPrivateKeyPath());
                }
            }

            // 获取SSH会话
            session = jsch.getSession(remoteConfig.getUsername(), remoteConfig.getHost(), remoteConfig.getPort());

            // 设置密码（如果没有使用私钥）
            if (remoteConfig.getPassword() != null && !remoteConfig.getPassword().trim().isEmpty()) {
                session.setPassword(remoteConfig.getPassword());
            }

            // 配置SSH会话
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            // 设置连接超时
            session.setTimeout(remoteConfig.getConnectionTimeout() * 1000);

            // 连接
            session.connect();
            log.info("SSH连接成功: {}@{}:{}", remoteConfig.getUsername(), remoteConfig.getHost(), remoteConfig.getPort());

            // 打开执行通道
            channel = session.openChannel("exec");
            ChannelExec execChannel = (ChannelExec) channel;

            // 设置要执行的命令
            execChannel.setCommand(command);

            // 设置错误流合并到输出流
            execChannel.setErrStream(System.err);

            // 获取输入流
            InputStream in = execChannel.getInputStream();
            
            // 连接通道
            execChannel.connect();

            // 读取命令输出
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            // 等待命令执行完成
            while (!execChannel.isClosed()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // 获取退出状态
            int exitStatus = execChannel.getExitStatus();
            String output = result.toString(StandardCharsets.UTF_8);
            
            log.info("SSH命令执行完成，退出状态: {}", exitStatus);
            log.debug("SSH命令输出: {}", output);

            if (exitStatus != 0) {
                throw new ServiceException("远程命令执行失败，退出状态: " + exitStatus + ", 输出: " + output);
            }

            return output;

        } catch (JSchException e) {
            log.error("SSH连接失败: {}", e.getMessage(), e);
            throw new ServiceException("SSH连接失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("SSH命令执行失败: {}", e.getMessage(), e);
            throw new ServiceException("SSH命令执行失败: " + e.getMessage());
        } finally {
            // 关闭连接
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    /**
     * 测试SSH连接
     *
     * @param remoteConfig 远程配置
     * @return 是否连接成功
     */
    public static boolean testConnection(PythonProperties.Remote remoteConfig) {
        try {
            executeRemoteCommand(remoteConfig, "echo 'SSH连接测试成功'");
            return true;
        } catch (Exception e) {
            log.error("SSH连接测试失败: {}", e.getMessage());
            return false;
        }
    }
} 