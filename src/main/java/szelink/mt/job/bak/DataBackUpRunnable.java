package szelink.mt.job.bak;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import szelink.mt.config.DataBackUpConfig;
import szelink.mt.entity.BinlogEventInfo;
import szelink.mt.entity.BinlogInfo;
import szelink.mt.entity.DataBackUpInfo;
import szelink.mt.event.EventChangeRunner;
import szelink.mt.service.BinlogEventService;
import szelink.mt.service.DataBackUpService;
import szelink.mt.util.*;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author mt
 * 定时器具体要做的事情
 * 备份思路:
 *  1.停掉mysql binlog的检测进程
 *  2.备份任务开始时,记录下数据库中最近一次入库的event信息
 *  3.备份文件,并处理多余的备份文件
 *  4,开启binlog 检测进程(从第2步记录的binlog名称和位置开始)
 */
public class DataBackUpRunnable implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DataBackUpRunnable.class);

    private ApplicationContext context = ApplicationContextProvider.getContext();

    private DataBackUpService backUpService = context.getBean(DataBackUpService.class);

    private BinlogEventService eventService = context.getBean(BinlogEventService.class);

    private DataBackUpConfig config = context.getBean(DataBackUpConfig.class);

    @Override
    public void run() {
        // 1.停止binlog的检测进程
        BinaryLogClient client = EventChangeRunner.getMonitorEventChange().getClient();
        closeClient(client);
        // 2.记录最后一次入库的 binlog event 信息
        BinlogEventInfo eventInfo = TemporaryVariable.LAST_TIME_EVENT;
        String lastFilename;
        Long lastPosition;
        if (eventInfo != null) {
            lastFilename = eventInfo.getBinlogFileName();
            lastPosition = eventInfo.getEndPosition();
        } else {

            BinlogInfo binlogInfo = JdbcUtils.queryBinlog();
            lastFilename = binlogInfo.getFileName();
            lastPosition = binlogInfo.getPosition();
        }
        logger.info("==========开始备份文件==========");
        // 3.1.备份mysql文件,并记录入库
        // 重置备份文件信息的id(此步骤必不可少)
        TemporaryVariable.DATA_BACKUP_ID = CommonUtil.uuid();
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String fileName = sdf.format(date);
        CompressType type;
        if (CommonUtil.isWindowsOs()) {
            type = CompressType.ZIP;
        } else {
            type = CompressType.TAR_GZ;
        }
        try {
            DataBackUpUtil.backup(config.getOriginPath(), config.getPath(), fileName, type);
        } catch (IOException e) {
            logger.error("==========压缩文件失败==========");
            e.printStackTrace();
        }
        // 3.2.生成备份信息,入库
        DataBackUpInfo info = new DataBackUpInfo();
        info.setId(TemporaryVariable.DATA_BACKUP_ID);
        info.setBackUpTime(date);
        info.setFileName(fileName);
        String filePath = config.getPath() + "\\" + fileName;
        info.setFilePath(filePath);
        info.setSize(new File(filePath).length());
        info.setBinlogFileName(lastFilename);
        info.setPosition(lastPosition);
        backUpService.save(info);
        // 3.3删除多余的备份文件
        if (backUpService.count() > config.getNum()) {
            logger.info("==========开始处理多余的备份文件==========");
            DataBackUpInfo lastTimeInfo = backUpService.getLastTimeInfo(info.getBackUpTime());
            List<DataBackUpInfo> infos = backUpService.getInfosLessThan(lastTimeInfo.getBackUpTime());
            for (DataBackUpInfo oldInfo : infos) {
                // 1)删除库中旧的备份信息
                backUpService.deleteById(oldInfo.getId());
                // 2)删除备份的文件
                FileUtils.deleteQuietly(new File(oldInfo.getFilePath()));
                // 3)删除和备份文件相关的event信息
                eventService.deleteByBakId(oldInfo.getId());
            }
            logger.info("==========完成处理多余的备份文件==========");
        }

        logger.info("==========完成文件备份==========");
        // 4.开启binlog 检测进程
        ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 2000L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(5));
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    client.setBinlogFilename(lastFilename);
                    client.setBinlogPosition(lastPosition);
                    client.connect();
                } catch (IOException e) {
                    logger.error("==========开启mysql binlog 检测进程失败");
                }
            }
        });
    }

    private void closeClient(BinaryLogClient client) {
        try {
            client.disconnect();
        } catch (IOException e) {
            logger.error("==========停止mysql binlog 检测进程失败==========");
            e.printStackTrace();
        }
    }
}
