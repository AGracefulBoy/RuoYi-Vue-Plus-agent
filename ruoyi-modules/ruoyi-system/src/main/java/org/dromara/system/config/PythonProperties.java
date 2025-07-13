package org.dromara.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Python配置属性
 *
 * @author ruoyi
 */
@Data
@Component
@ConfigurationProperties(prefix = "python")
public class PythonProperties {

    /**
     * 执行模式：local-本地执行, remote-远程执行
     */
    private String executionMode = "local";

    /**
     * 本地模式配置
     */
    private Local local = new Local();

    /**
     * 远程模式配置
     */
    private Remote remote = new Remote();

    /**
     * 本地执行配置
     */
    @Data
    public static class Local {
        /**
         * 虚拟环境路径
         */
        private String virtualenvPath = "./py-runtime";

        /**
         * 激活脚本路径
         */
        private String activateScript = "./myenv/bin/activate";
    }

    /**
     * 远程执行配置
     */
    @Data
    public static class Remote {
        /**
         * 远程服务器地址
         */
        private String host = "localhost";

        /**
         * SSH端口
         */
        private int port = 22;

        /**
         * 用户名
         */
        private String username = "root";

        /**
         * 密码（可选，建议使用密钥）
         */
        private String password;

        /**
         * 私钥路径（可选）
         */
        private String privateKeyPath;

        /**
         * 私钥密码（可选）
         */
        private String privateKeyPassphrase;

        /**
         * 连接超时时间（秒）
         */
        private int connectionTimeout = 30;

        /**
         * 远程虚拟环境路径
         */
        private String virtualenvPath = "/opt/py-runtime";

        /**
         * 远程激活脚本路径
         */
        private String activateScript = "/opt/myenv/bin/activate";
    }

    /**
     * 获取当前激活脚本路径
     */
    public String getActivateScript() {
        return "remote".equals(executionMode) ? remote.getActivateScript() : local.getActivateScript();
    }

    /**
     * 获取当前虚拟环境路径
     */
    public String getVirtualenvPath() {
        return "remote".equals(executionMode) ? remote.getVirtualenvPath() : local.getVirtualenvPath();
    }

    /**
     * 是否为远程模式
     */
    public boolean isRemoteMode() {
        return "remote".equals(executionMode);
    }

} 