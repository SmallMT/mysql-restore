package szelink.mt.event;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import szelink.mt.config.DataBackUpConfig;
import szelink.mt.constant.ConfigConstant;
import szelink.mt.entity.BinlogEventInfo;
import szelink.mt.entity.BinlogInfo;
import szelink.mt.entity.DataBackUpInfo;
import szelink.mt.lister.BinlogEventListener;
import szelink.mt.service.BinlogEventService;
import szelink.mt.service.DataBackUpService;
import szelink.mt.util.*;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author mt
 * 处理binlog文件堆积问题
 */
public class DealWithAccumulation {

    private static final Logger logger = LoggerFactory.getLogger(DealWithAccumulation.class);

    private ApplicationContext context = ApplicationContextProvider.getContext();

    private DataBackUpConfig config = context.getBean(DataBackUpConfig.class);

    private DataBackUpService backUpService = context.getBean(DataBackUpService.class);

    private BinlogEventService eventService = context.getBean(BinlogEventService.class);

    private BinaryLogClient newClient() {
        return new BinaryLogClient(ConfigConstant.BINLOG_SERVER_ADDRESS, ConfigConstant.BINLOG_SERVER_PORT,
                ConfigConstant.BINLOG_SERVER_USERNAME, ConfigConstant.BINLOG_SERVER_PASSWORD);
    }

    /**
     * 解决未处理的binlog信息
     */
    void dealWithAccumulation() {
        logger.info("==========开始处理堆积的未处理的binlog文件==========");
        BinlogInfo binlogInfo = JdbcUtils.queryBinlog();
        BinlogEventInfo recentEvent = eventService.recentEvent();
        if (recentEvent == null) {
            // 数据库中还没有任何binlog event信息
            // 将目前所有的binlog文件进行解析(直到binlogInfo 中的位置)
//            connect(CustomizeConstant.FIRST_BINLOG_NAME, CustomizeConstant.BINLOG_START_POSITION, binlogInfo.getFileName(), binlogInfo.getPosition());
//            logger.info("==========完成对堆积的binlog文件处理工作==========");
            return;
        }
        String recentFileName = recentEvent.getBinlogFileName();
        Long recentPosition = recentEvent.getEndPosition();
        String nowFileName = binlogInfo.getFileName();
        Long nowPosition = binlogInfo.getPosition();
        connect(recentFileName, recentPosition, nowFileName, nowPosition);
        logger.info("==========完成对堆积的binlog文件处理工作==========");
    }

    void backAfterStart() {
        BinlogInfo binlogInfo = JdbcUtils.queryBinlog();
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
        info.setBinlogFileName(binlogInfo.getFileName());
        info.setPosition(binlogInfo.getPosition());
        backUpService.save(info);
    }

    private void connect(String startBinlogName, Long startPos, String stopBinlogName, Long stopPos) {
        BinaryLogClient client = newClient();
        client.setBinlogFilename(startBinlogName);
        client.setBinlogPosition(startPos);
        client.setBlocking(false);
        client.registerEventListener(new BinlogEventListener(stopBinlogName,stopPos));
        try {
            client.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
