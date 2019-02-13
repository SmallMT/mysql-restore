package szelink.mt.constant;

import org.springframework.util.ResourceUtils;

import java.io.IOException;

/**
 * @author mt
 * 自定义的一些常量
 */
public class CustomizeConstant {

    /**
     * 当sql语句的大小超出了这个值就认为是大sql语句
     */
    public static final Integer BIG_SQL_SIZE = 2 << 10;

    /** window 操作系统*/
    public static final String WINDOWS_OS = "windows";

    /** linux 操作系统*/
    public static final String LINUX_OS = "linux";

    /** 首个binlog文件名称*/
    public static final String FIRST_BINLOG_NAME = "mysql-bin.000001";

    /**每个binlog文件正文开始位置*/
    public static final Long BINLOG_START_POSITION = 120L;

    /** DML sql语句中 QUERY事件的内容*/
    public static final String DML_SQL = "BEGIN";

    /**非法(经过加密的)的sql语句都会以此开头*/
    public static final String ILLEAL_SQL = "BINLOG";

    /** 项目绝对路径*/
    public static String PROJECT_URL;

    public static final String ZIP = ".zip";

    public static final String TAR_GZ = ".tar.gz";


    static {
        try {
            PROJECT_URL = ResourceUtils.getURL("classpath:").getPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
