package szelink.mt.util;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import szelink.mt.config.DataBackUpConfig;
import szelink.mt.constant.CustomizeConstant;
import szelink.mt.entity.BinlogEventInfo;
import szelink.mt.entity.BinlogFileInfo;
import szelink.mt.entity.BinlogInfo;
import szelink.mt.event.EventChangeRunner;
import szelink.mt.service.BinlogEventService;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author mt
 * 数据恢复 工具类
 * 该类设计存在问题
 * 该工具类与项目耦合度太高
 */
@Component
public class RepairData {

    private static final Logger logger = LoggerFactory.getLogger(RepairData.class);

    @Autowired
    @Qualifier("backUpConfig")
    private DataBackUpConfig config;

    @Autowired
    @Qualifier("binlogEventService")
    private BinlogEventService eventService;

    /**
     * 从bakFile 备份文件中恢复从[beginBinlog.beginPos,endBinlog.endPos]区间内的数据
     *
     * @param bakFile 已经备份的文件
     * @param beginBinlog 起始binlog(简单文件名eg. mysql-bin.000001)
     * @param beginPos 起始binlog位置
     * @param endBinlog 结束binlog(简单文件名eg. mysql-bin.000002)
     * @param endPos 结束binlog位置
     */
    public void repair(File bakFile, String beginBinlog, Long beginPos, String endBinlog, Long endPos) {
        // 1.删除上一次恢复数据时执行的sql文件
        if (TemporaryVariable.LAST_TIME_REPAIR_SQL_FILE != null) {
            deleteLastSqlFile();
        }
        // 2.记录当前的执行的sql文件夹路径
        String sqlDirName = CommonUtil.uuid();
        String file = config.getPath() + "sql" + File.separator;
        File sqlRoot = new File(file);
        if (!sqlRoot.exists()) {
            sqlRoot.mkdir();
        }
        String curSqlFile = file + sqlDirName + File.separator;
        TemporaryVariable.LAST_TIME_REPAIR_SQL_FILE = curSqlFile;
        File sqlDir = new File(curSqlFile);
        if (!sqlDir.exists()) {
            sqlDir.mkdir();
        }
        // 3.从原来的binlog文件中导出需要恢复的sql语句
        exportSql(beginBinlog, beginPos, endBinlog, endPos, curSqlFile);
        restore(bakFile, sqlDir);
    }


    public void repair(File bakFile, String beginBinlog, Long beginPos, BinlogEventInfo reference, List<BinlogEventInfo> excludes) {
        // 1.删除上一次恢复数据时执行的sql文件
        if (TemporaryVariable.LAST_TIME_REPAIR_SQL_FILE != null) {
            deleteLastSqlFile();
        }
        // 2.记录当前的执行的sql文件夹路径
        String sqlDirName = CommonUtil.uuid();
        String file = config.getPath() + "sql" + File.separator;
        File sqlRoot = new File(file);
        if (!sqlRoot.exists()) {
            sqlRoot.mkdir();
        }
        String curSqlFile = file + sqlDirName + File.separator;
        TemporaryVariable.LAST_TIME_REPAIR_SQL_FILE = curSqlFile;
        File sqlDir = new File(curSqlFile);
        if (!sqlDir.exists()) {
            sqlDir.mkdir();
        }
        // 3.导出sql
        for (BinlogEventInfo event : excludes) {
            exportSql(beginBinlog, beginPos, event.getBinlogFileName(), event.getStartPosition(), curSqlFile);
            beginBinlog = event.getBinlogFileName();
            beginPos = event.getEndPosition();
        }
        exportSql(beginBinlog, beginPos, reference.getBinlogFileName(), reference.getEndPosition(), curSqlFile);
        // 4.恢复数据
        restore(bakFile, sqlDir);
    }

    private void exportSql(String beginBinlog, Long beginPos, String endBinlog, Long endPos, String sqlDir) {
        String filling = Long.toString(System.currentTimeMillis());
        if (beginBinlog.equalsIgnoreCase(endBinlog)) {
            // 起始位置和结束位置在同一个文件中
            String absoluteBeginBinlog = config.getOriginPath() + "\\" + beginBinlog;
            String destFile = sqlDir + File.separator + beginBinlog + "-" + filling + ".sql";
            JdbcUtils.exportSql(absoluteBeginBinlog, beginPos, endPos, destFile);
        } else {
            // 起始位置和结束位置不在同一个文件中
            List<BinlogFileInfo> indexs = JdbcUtils.binlogIndexInfo();
            int index = 0;
            for (int i = 0; i < indexs.size(); i++) {
                if (beginBinlog.equalsIgnoreCase(indexs.get(i).getBinlogName())) {
                    index = i;
                    break;
                }
            }
            for (int i = index; i < indexs.size(); i++) {
                BinlogFileInfo tempInfo = indexs.get(i);
                String destFile = sqlDir + File.separator + tempInfo.getBinlogName() + "-" + filling + ".sql";
                String allBinlogName = config.getOriginPath() + "\\" + tempInfo.getBinlogName();
                if (i == index) {
                    // 第一个
                    JdbcUtils.exportSql(allBinlogName, beginPos, tempInfo.getSize(), destFile);
                } else if (i == indexs.size() - 1) {
                    // 最后一个
                    JdbcUtils.exportSql(allBinlogName, CustomizeConstant.BINLOG_START_POSITION, endPos, destFile);
                } else {
                    // 其余的
                    JdbcUtils.exportSql(allBinlogName, CustomizeConstant.BINLOG_START_POSITION, tempInfo.getSize(), destFile);
                }
            }
        }
    }

    private void restore(File bakFile, File sqlDir) {
        BinaryLogClient client = EventChangeRunner.getMonitorEventChange().getClient();
        closeBinlogClient(client);
        // 5.停止数据库服务
        JdbcUtils.stopMysql();
        // 6.解压之前备份好的压缩文件来替换原data文件夹
        replaceData(bakFile, config.getOriginPath());
        // 7.重启mysql服务
        JdbcUtils.startMysql();
        // 8.清除h2数据库中的相关数据
        eventService.clear();
        // 9.重置mysql binlog监测工具
        startBinlogClient(client);
        // 10.执行导出的sql文件,完成数据还原
        JdbcUtils.executeSql(sqlDir);
    }


    private void deleteLastSqlFile() {
        if (TemporaryVariable.LAST_TIME_REPAIR_SQL_FILE != null) {
            try {
                File file = new File(TemporaryVariable.LAST_TIME_REPAIR_SQL_FILE);
                if (file.isDirectory()) {
                    FileUtils.deleteDirectory(file);
                } else {
                    FileUtils.deleteQuietly(file);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 替换原数据库data文件夹
     * @param src 需要替换的压缩文件
     * @param path 原数据库data目录
     */
    private void replaceData(File src, String path) {
        // 1.删除原来的文件夹
        FileUtils.deleteQuietly(new File(path));
        // 2.用新的文件夹填充原来的文件夹
        FileCompressUtil.unZip(src, path);
    }


    private void closeBinlogClient(BinaryLogClient client) {
        try {
            logger.info("==========正在关闭mysql binlog监测服务...==========");
            client.disconnect();
            logger.info("==========成功关闭mysql binlog监测服务==========");
        } catch (IOException e) {
            logger.error("==========关闭mysql监测服务失败==========");
            e.printStackTrace();
        }
    }

    private void startBinlogClient(BinaryLogClient client) {
        BinlogInfo info = JdbcUtils.queryBinlog();
        client.setBinlogFilename(info.getFileName());
        client.setBinlogPosition(info.getPosition());
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    logger.info("==========正在开启mysql binlog服务...==========");
                    client.connect();
                    logger.info("==========成功开启mysql binlog服务==========");
                } catch (IOException e) {
                    logger.error("==========开启mysql binlog服务失败==========");
                    e.printStackTrace();
                }
            }
        });
        th.start();
        try {
            Thread.sleep(3000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
