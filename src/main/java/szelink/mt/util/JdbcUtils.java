package szelink.mt.util;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import szelink.mt.constant.ConfigConstant;
import szelink.mt.entity.BinlogFileInfo;
import szelink.mt.entity.BinlogInfo;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.*;
import java.util.*;

/**
 * @author mt 2018-11-26
 * 数据库备份的时候,需要操作数据库来获取些许相关的binlog信息,所以需要操作数据库.
 * 只是进行少许的查询操作,且长时间才会操作一次.所以没有使用连接池,采取了原生的jdbc的方式
 * 获取连接->查询->释放连接
 */
public final class JdbcUtils {


    private static final Logger logger = LoggerFactory.getLogger(JdbcUtils.class);

    private static final String BINLOG_INFO_SQL = "show master status;";

    private static final String BINLOG_INDEX_SQL = "show master logs;";

    private static final String SHOW_DATABASES = "show databases;";

    private static final String DROP_DATABASE = "drop database ";

    private static final String[] BUILT_IN_DATABASE = {"information_schema","mysql","performance_schema"};



    private static Connection conn;

    private static void getJdbcConnection() {
        try {
            Class.forName(ConfigConstant.DATASOURCE_DRIVER);
            conn = DriverManager.getConnection(ConfigConstant.DATASOURCE_URL, ConfigConstant.DATASOURCE_USERNAME,
                    ConfigConstant.DATASOURCE_PASSWORD);
        } catch (ClassNotFoundException e) {
            logger.error("======数据库驱动类型  " + ConfigConstant.DATASOURCE_URL + "  错误======");
        } catch (SQLException e) {
            logger.error("======获取数据库连接失败======");
        }
        if (conn == null) {
            throw new NullPointerException("======获取连接失败======");
        }
    }

    /**
     * 导出某个binlog某个区间内的所有sql语句
     * @param binlog binlog文件的完成名称(完成路径+完成名称 eg.d:\\xx\\xx\\mysql-bin.000001)
     * @param beginPos 起始位置
     * @param endPos 结束位置
     * @param destFile 导出sql文件的路径
     */
    public static void exportSql(String binlog, Long beginPos, Long endPos, String destFile) {
        String mysqlbinlog = ConfigConstant.MYSQL_BIN_PATH+"mysqlbinlog";
        String command = "cmd /C " + mysqlbinlog + " --start-position=" + beginPos +
                " --stop-position=" + endPos + " " + binlog + " >" + destFile;
        try {
            Runtime.getRuntime().exec(command).waitFor();
        } catch (Exception e) {
            logger.error("==========导出sql语句失败==========");
            e.printStackTrace();
        }
    }



    /**
     * 执行某个sql文件(xxx.sql)或某个文件夹下的所有sql文件
     * @param fileOrDir
     */
    public static void executeSql(File fileOrDir) {
        if (fileOrDir == null) {
            throw new NullPointerException("需要执行的sql文件不存在");
        }
        File[] sqlFiles;
        if (fileOrDir.isDirectory()) {
            sqlFiles = fileOrDir.listFiles();
        } else {
            sqlFiles = new File[1];
            sqlFiles[0] = fileOrDir;
        }
        String mysql = ConfigConstant.MYSQL_BIN_PATH+"mysql";
        String address = ConfigConstant.BINLOG_SERVER_ADDRESS;
        String user = ConfigConstant.BINLOG_SERVER_USERNAME;
        String password = ConfigConstant.BINLOG_SERVER_PASSWORD;
        Integer port = ConfigConstant.BINLOG_SERVER_PORT;
        String[] commands = new String[2];
        if (CommonUtil.isWindowsOs()) {
            commands[0] = "cmd /C " + mysql + " -h" + address + " -u" + user +
                    " -p" + password + " -P" + port;
            commands[1] = "source ";
        } else {
            // 预留的linux命令
        }
        try {
            logger.info("==========开始执行sql文件==========");
            Process process = Runtime.getRuntime().exec(commands[0]);
            OutputStream out = process.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out);
            for (File file : sqlFiles) {
                String content = commands[1] + file.getAbsolutePath();
                writer.write(content);
                writer.write(System.getProperty("line.separator"));
                writer.flush();
            }
            writer.close();
            out.close();
            logger.info("==========成功执行sql文件==========");
        } catch (Exception e) {
            logger.error("==========执行sql文件失败==========");
            e.printStackTrace();
        }
    }

    /**
     * 查询当前binlog信息(当前binlog文件,当前位置)
     * @return
     */
    public static BinlogInfo queryBinlog() {
        getJdbcConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(BINLOG_INFO_SQL);
            ResultSet rs = ps.executeQuery();
            rs.next();
            BinlogInfo binlogInfo = new BinlogInfo();
            binlogInfo.setFileName(rs.getString("File"));
            binlogInfo.setPosition(rs.getLong("Position"));
            return binlogInfo;
        } catch (SQLException e) {
            logger.error("==========执行sql语句  " + BINLOG_INFO_SQL + "  出错==========");
            e.printStackTrace();
        }
        close();
        return null;
    }

    public static void binlogEvents(String binlogName, Long position) {
        getJdbcConnection();
        String sql = "show binlog events in \"" + binlogName + "\"" + " from " + position;
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                System.out.println(rs.getString("Log_name"));
                System.out.println(rs.getLong("Pos"));
                System.out.println(rs.getString("Event_type"));
                System.out.println(rs.getLong("Server_id"));
                System.out.println(rs.getLong("End_log_pos"));
                System.out.println(rs.getString("Info"));
            }
        } catch (SQLException e) {
            logger.error("======预编译sql语句  " + sql + "  出错");
            e.printStackTrace();
        }
        close();
    }

    /**
     * 获取当前所有的binlog文件信息(binlog名称,binlog文件大小)
     * @return
     */
    public static List<BinlogFileInfo> binlogIndexInfo() {
        List<BinlogFileInfo> infos = new ArrayList<>(10);
        getJdbcConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(BINLOG_INDEX_SQL);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                BinlogFileInfo info = new BinlogFileInfo();
                info.setBinlogName(rs.getString("Log_name"));
                info.setSize(rs.getLong("File_size"));
                infos.add(info);
            }
        } catch (SQLException e) {
            logger.error("======预编译sql语句  " + BINLOG_INDEX_SQL + "  出错");
            e.printStackTrace();
        }
        close();
        return infos;
    }

    public static void stopMysql() {
        String command = "";
        if (CommonUtil.isWindowsOs()) {
            command = "cmd /C net stop " + ConfigConstant.MYSQL_WINDOWS_SERVER_NAME;
        } else {
            // linux预留操作
        }
        try {
            logger.info("==========正在关闭mysql服务...==========");
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            logger.info("==========成功关闭mysql服务==========");
        } catch (Exception e) {
            logger.error("==========关闭mysql服务失败==========");
            e.printStackTrace();
        }
    }

    public static void startMysql() {
        String command = "";
        if (CommonUtil.isWindowsOs()) {
            command = "cmd /C net start " + ConfigConstant.MYSQL_WINDOWS_SERVER_NAME;
        } else {
            // linux预留操作
        }
        try {
            logger.info("==========正在启动mysql服务...==========");
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            logger.info("==========成功启动mysql服务==========");
        } catch (Exception e) {
            logger.error("==========启动mysql服务失败==========");
            e.printStackTrace();
        }
    }

    /**
     * 获取数据库中所有的database(不包含内置数据库)
     * @return
     */
    public static List<String> databases() {
        getJdbcConnection();
        List<String> databases = new ArrayList<>(10);
        List<String> buildIn = Arrays.asList(BUILT_IN_DATABASE);
        try {
            PreparedStatement ps = conn.prepareStatement(SHOW_DATABASES);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String database = rs.getString("Database");
                if (!buildIn.contains(database)) {
                    databases.add(database);
                }
            }
        } catch (SQLException e) {
            logger.error("======预编译sql语句  " + SHOW_DATABASES + "  出错");
            e.printStackTrace();
        }
        close();
        return databases;
    }

    /**
     * 危险操作,谨慎使用
     * 删除某个数据库()
     * @param database
     */
    public static void dropDatabase(String database) {
        String sql = DROP_DATABASE + database;
        getJdbcConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            logger.info("==========执行sql语句 : " + sql +"==========");
            ps.execute();
        } catch (SQLException e) {
            logger.error("======预编译sql语句  " + sql + "  出错");
            e.printStackTrace();
        }
        close();
    }


    public static void exportOneDB(String database, String origin, String dest, String beginning) throws IOException{
        List<String> databases = databases();
        databases.remove(database);
        // 删除除需要导出之外的所有数据库
        for (String db : databases) {
            dropDatabase(db);
        }
        stopMysql();
        // 导出数据库压缩文件
        DataBackUpUtil.backup(origin, dest, database);
        // 将数据库状态恢复到刚开始的状态
        FileUtils.deleteDirectory(new File(origin));
        FileCompressUtil.unZip(new File(beginning), origin);
        // 重启数据库
        startMysql();
    }


    private static void close() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.error("======释放数据库连接失败======");
            }
        }
    }

    public static void main(String[] args) throws IOException{
        String oir = "D:\\MySql\\data\\data";
        String dest = "E:\\databak";
        DataBackUpUtil.backup(oir, dest, "origin");

        JdbcUtils.exportOneDB("e4safe_new_gd", "D:\\MySql\\data\\data", dest, "E:\\databak\\origin.zip");

    }

}
