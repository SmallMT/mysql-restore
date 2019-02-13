package szelink.mt.constant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * @author mt
 * 配置文件config.properties
 */
public final class ConfigConstant {

    private static final Logger logger = LoggerFactory.getLogger(ConfigConstant.class);

    private ConfigConstant(){}

    private static final String CONFIG_FILE_PATH = "config/config.properties";

    public static String BINLOG_SERVER_ADDRESS;

    public static Integer BINLOG_SERVER_PORT;

    public static String BINLOG_SERVER_USERNAME;

    public static String BINLOG_SERVER_PASSWORD;

    public static String DATASOURCE_URL;

    public static String DATASOURCE_USERNAME;

    public static String DATASOURCE_PASSWORD;

    public static String DATASOURCE_DRIVER;

    public static String MYSQL_BIN_PATH;

    public static String MYSQL_WINDOWS_SERVER_NAME;

    static {
        try {
            File configFile = ResourceUtils.getFile("classpath:" + CONFIG_FILE_PATH);
            Properties properties = new Properties();
            properties.load(new FileInputStream(configFile));
            BINLOG_SERVER_ADDRESS = properties.getProperty("mysql-binlog-address");
            BINLOG_SERVER_PORT = Integer.parseInt(properties.getProperty("mysql-binlog-port"));
            BINLOG_SERVER_USERNAME = properties.getProperty("mysql-binlog-username");
            BINLOG_SERVER_PASSWORD = properties.getProperty("mysql-binlog-password");
            DATASOURCE_URL = properties.getProperty("datasource-url");
            DATASOURCE_USERNAME = properties.getProperty("datasource-username");
            DATASOURCE_PASSWORD = properties.getProperty("datasource-password");
            DATASOURCE_DRIVER = properties.getProperty("datasource-driver");
            MYSQL_BIN_PATH = properties.getProperty("mysql-bin-path");
            MYSQL_WINDOWS_SERVER_NAME = properties.getProperty("mysql-windows-server-name");
        } catch (Exception e) {
            logger.error("========== 找不到 " + CONFIG_FILE_PATH + " 文件==========");
            logger.error("==========初始化参数失败,常量将全部设置为 null==========");
            e.printStackTrace();
        }
    }


}
