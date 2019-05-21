package szelink.mt.constant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * @author mt
 * 配置文件config.properties
 */
public final class ConfigConstant {

    private static final Logger logger = LoggerFactory.getLogger(ConfigConstant.class);

    private ConfigConstant() {

        throw new AssertionError("No ConfigConstant Instance For You");
    }

    /**
     * 配置文件的相对路径
     */
    private static final String CONFIG_FILE_PATH = "config/config.properties";

    /**
     * binlog 服务地址(一般是mysql的地址 例如本地地址 127.0.0.1)
     */
    public static String BINLOG_SERVER_ADDRESS;

    /**
     * binlog 服务端口(一般是mysql的端口)
     */
    public static int BINLOG_SERVER_PORT;

    /**
     * binlog 服务用户名(一般是mysql的用户名,这里需要使用一个权限较大的用户,例如root用户)
     */
    public static String BINLOG_SERVER_USERNAME;

    /**
     * binlog 服务密码(一般是mysql的密码)
     */
    public static String BINLOG_SERVER_PASSWORD;


    /**
     * 数据库url
     */
    public static String DATASOURCE_URL;

    /**
     * 数据库用户名
     */
    public static String DATASOURCE_USERNAME;

    /**
     * 数据库密码
     */
    public static String DATASOURCE_PASSWORD;

    /**
     * 数据库driver
     */
    public static String DATASOURCE_DRIVER;

    /**
     * 数据库mysql binlog存放的目录
     */
    public static String MYSQL_BIN_PATH;

    /**
     * mysql 在window 服务中注册的服务名称
     */
    public static String MYSQL_WINDOWS_SERVER_NAME;

    static {
        try {
            File configFile = ResourceUtils.getFile("classpath:" + CONFIG_FILE_PATH);
            Properties properties = new Properties();
            properties.load(new FileInputStream(configFile));
            BINLOG_SERVER_ADDRESS = properties.getProperty("datasource-address");
            BINLOG_SERVER_PORT = Integer.parseInt(properties.getProperty("datasource-port"));
            BINLOG_SERVER_USERNAME = properties.getProperty("datasource-username");
            BINLOG_SERVER_PASSWORD = properties.getProperty("datasource-password");
            DATASOURCE_URL = properties.getProperty("datasource-url");
            DATASOURCE_USERNAME = properties.getProperty("datasource-username");
            DATASOURCE_PASSWORD = properties.getProperty("datasource-password");
            DATASOURCE_DRIVER = properties.getProperty("datasource-driver");
            MYSQL_BIN_PATH = properties.getProperty("mysql-bin-path");
            MYSQL_WINDOWS_SERVER_NAME = properties.getProperty("mysql-windows-server-name");
        } catch (FileNotFoundException e) {
            logger.error("========== 找不到 " + CONFIG_FILE_PATH + " 文件,初始化参数失败!!==========");

        } catch (IOException e) {
            logger.error("==========  载入文件 "+ CONFIG_FILE_PATH + " 失败!!==========");
        }
    }


}
