package org.dromara.system.util;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.system.domain.bo.SysDatasourceBo;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源验证工具类
 *
 * @author ruoyi
 */
public class DatasourceValidatorUtils {

    /**
     * 支持的数据库类型及其默认端口
     */
    private static final Map<String, Integer> DATABASE_DEFAULT_PORTS = new HashMap<>();
    
    /**
     * 支持的数据库类型及其驱动类名
     */
    private static final Map<String, String> DATABASE_DRIVER_CLASSES = new HashMap<>();

    static {
        // 初始化数据库默认端口
        DATABASE_DEFAULT_PORTS.put("mysql", 3306);
        DATABASE_DEFAULT_PORTS.put("postgresql", 5432);
        DATABASE_DEFAULT_PORTS.put("oracle", 1521);
        DATABASE_DEFAULT_PORTS.put("sqlserver", 1433);
        DATABASE_DEFAULT_PORTS.put("clickhouse", 8123);
        DATABASE_DEFAULT_PORTS.put("sqlite", 0); // SQLite 不需要端口

        // 初始化数据库驱动类名
        DATABASE_DRIVER_CLASSES.put("mysql", "com.mysql.cj.jdbc.Driver");
        DATABASE_DRIVER_CLASSES.put("postgresql", "org.postgresql.Driver");
        DATABASE_DRIVER_CLASSES.put("oracle", "oracle.jdbc.driver.OracleDriver");
        DATABASE_DRIVER_CLASSES.put("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        DATABASE_DRIVER_CLASSES.put("clickhouse", "com.clickhouse.jdbc.ClickHouseDriver");
        DATABASE_DRIVER_CLASSES.put("sqlite", "org.sqlite.JDBC");
    }

    /**
     * 验证数据源配置
     *
     * @param bo 数据源配置
     * @return 验证结果信息，null表示验证通过
     */
    public static String validateDatasource(SysDatasourceBo bo) {
        if (bo == null) {
            return "数据源配置不能为空";
        }

        // 验证基础字段
        String basicValidation = validateBasicFields(bo);
        if (StringUtils.isNotBlank(basicValidation)) {
            return basicValidation;
        }

        // 根据数据源类型进行具体验证
        if ("database".equals(bo.getDatasourceType())) {
            return validateDatabaseDatasource(bo);
        } else if ("excel".equals(bo.getDatasourceType())) {
            return validateExcelDatasource(bo);
        }

        return "不支持的数据源类型：" + bo.getDatasourceType();
    }

    /**
     * 验证基础字段
     */
    private static String validateBasicFields(SysDatasourceBo bo) {
        if (StringUtils.isBlank(bo.getDatasourceName())) {
            return "数据源名称不能为空";
        }

        if (StringUtils.isBlank(bo.getDatasourceType())) {
            return "数据源类型不能为空";
        }

        if (!"database".equals(bo.getDatasourceType()) && !"excel".equals(bo.getDatasourceType())) {
            return "数据源类型只能是database或excel";
        }

        return null;
    }

    /**
     * 验证数据库类型数据源
     */
    private static String validateDatabaseDatasource(SysDatasourceBo bo) {
        // 验证数据库类型
        if (StringUtils.isBlank(bo.getDatabaseType())) {
            return "数据库类型不能为空";
        }

        String databaseType = bo.getDatabaseType().toLowerCase();
        if (!DATABASE_DEFAULT_PORTS.containsKey(databaseType)) {
            return "不支持的数据库类型：" + bo.getDatabaseType();
        }

        // SQLite 特殊处理
        if ("sqlite".equals(databaseType)) {
            return validateSqliteDatasource(bo);
        }

        // 其他数据库类型验证
        if (StringUtils.isBlank(bo.getHost())) {
            return "数据库主机地址不能为空";
        }

        if (ObjectUtil.isNull(bo.getPort())) {
            return "数据库端口号不能为空";
        }

        if (bo.getPort() < 1 || bo.getPort() > 65535) {
            return "数据库端口号必须在1-65535之间";
        }

        if (StringUtils.isBlank(bo.getDatabaseName())) {
            return "数据库名称不能为空";
        }

        if (StringUtils.isBlank(bo.getUsername())) {
            return "数据库用户名不能为空";
        }

        // 验证驱动类名
        if (StringUtils.isBlank(bo.getDriverClassName())) {
            // 如果未指定驱动类名，使用默认值
            String defaultDriver = DATABASE_DRIVER_CLASSES.get(databaseType);
            if (StringUtils.isNotBlank(defaultDriver)) {
                bo.setDriverClassName(defaultDriver);
            }
        }

        // 验证连接参数
        return validateConnectionParams(bo);
    }

    /**
     * 验证SQLite数据源
     */
    private static String validateSqliteDatasource(SysDatasourceBo bo) {
        if (StringUtils.isBlank(bo.getDatabaseName())) {
            return "SQLite数据库文件路径不能为空";
        }

        // 验证文件路径格式
        String dbPath = bo.getDatabaseName();
        if (!dbPath.toLowerCase().endsWith(".db") && !dbPath.toLowerCase().endsWith(".sqlite") 
            && !dbPath.toLowerCase().endsWith(".sqlite3")) {
            return "SQLite数据库文件必须是.db、.sqlite或.sqlite3格式";
        }

        return null;
    }

    /**
     * 验证Excel文件类型数据源
     */
    private static String validateExcelDatasource(SysDatasourceBo bo) {
        if (StringUtils.isBlank(bo.getFilePath())) {
            return "Excel文件路径不能为空";
        }

        String filePath = bo.getFilePath().toLowerCase();
        if (!filePath.endsWith(".xlsx") && !filePath.endsWith(".xls")) {
            return "文件必须是Excel格式(.xlsx或.xls)";
        }

        // 验证文件是否存在（如果是绝对路径）
        if (isAbsolutePath(bo.getFilePath())) {
            File file = new File(bo.getFilePath());
            if (!file.exists()) {
                return "指定的Excel文件不存在：" + bo.getFilePath();
            }
            if (!file.isFile()) {
                return "指定的路径不是文件：" + bo.getFilePath();
            }
            if (!file.canRead()) {
                return "无法读取指定的Excel文件：" + bo.getFilePath();
            }
            
            // 设置文件大小
            bo.setFileSize(file.length());
        }

        return null;
    }

    /**
     * 验证连接参数
     */
    private static String validateConnectionParams(SysDatasourceBo bo) {
        // 验证连接超时时间
        if (ObjectUtil.isNotNull(bo.getConnectionTimeout()) && bo.getConnectionTimeout() < 1000) {
            return "连接超时时间不能少于1000毫秒";
        }

        // 验证查询超时时间
        if (ObjectUtil.isNotNull(bo.getQueryTimeout()) && bo.getQueryTimeout() < 1000) {
            return "查询超时时间不能少于1000毫秒";
        }

        // 验证最大连接数
        if (ObjectUtil.isNotNull(bo.getMaxConnections()) && bo.getMaxConnections() < 1) {
            return "最大连接数必须大于0";
        }

        return null;
    }

    /**
     * 判断是否为绝对路径
     */
    private static boolean isAbsolutePath(String path) {
        if (StringUtils.isBlank(path)) {
            return false;
        }
        
        // Windows 绝对路径: C:\ 或 \\server\share
        if (path.length() >= 3 && path.charAt(1) == ':' && path.charAt(2) == '\\') {
            return true;
        }
        if (path.startsWith("\\\\")) {
            return true;
        }
        
        // Unix/Linux 绝对路径: /path
        return path.startsWith("/");
    }

    /**
     * 获取数据库类型的默认端口
     */
    public static Integer getDefaultPort(String databaseType) {
        if (StringUtils.isBlank(databaseType)) {
            return null;
        }
        return DATABASE_DEFAULT_PORTS.get(databaseType.toLowerCase());
    }

    /**
     * 获取数据库类型的默认驱动类名
     */
    public static String getDefaultDriverClassName(String databaseType) {
        if (StringUtils.isBlank(databaseType)) {
            return null;
        }
        return DATABASE_DRIVER_CLASSES.get(databaseType.toLowerCase());
    }

    /**
     * 判断是否为支持的数据库类型
     */
    public static boolean isSupportedDatabaseType(String databaseType) {
        if (StringUtils.isBlank(databaseType)) {
            return false;
        }
        return DATABASE_DEFAULT_PORTS.containsKey(databaseType.toLowerCase());
    }

    /**
     * 生成数据库连接URL
     */
    public static String buildConnectionUrl(SysDatasourceBo bo) {
        if (StringUtils.isNotBlank(bo.getConnectionUrl())) {
            return bo.getConnectionUrl();
        }

        String databaseType = bo.getDatabaseType().toLowerCase();
        String host = bo.getHost();
        Integer port = bo.getPort();
        String databaseName = bo.getDatabaseName();

        return switch (databaseType) {
            case "mysql" -> String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai",
                    host, port, databaseName);
            case "postgresql" -> String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
            case "oracle" -> String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, databaseName);
            case "sqlserver" -> String.format("jdbc:sqlserver://%s:%d;databaseName=%s", host, port, databaseName);
            case "clickhouse" -> String.format("jdbc:clickhouse://%s:%d/%s", host, port, databaseName);
            case "sqlite" -> String.format("jdbc:sqlite:%s", databaseName);
            default -> throw new IllegalArgumentException("不支持的数据库类型: " + databaseType);
        };
    }

} 