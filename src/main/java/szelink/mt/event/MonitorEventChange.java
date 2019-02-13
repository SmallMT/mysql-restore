package szelink.mt.event;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import szelink.mt.constant.ConfigConstant;
import szelink.mt.entity.BinlogEventInfo;
import szelink.mt.entity.BinlogInfo;
import szelink.mt.lister.BinlogEventListener;
import szelink.mt.service.BinlogEventService;
import szelink.mt.util.ApplicationContextProvider;
import szelink.mt.util.JdbcUtils;

/**
 * @author mt
 * 监测mysql binlog 的变动情况
 * 此类只有在项目启动的时候才会去new
 */
public class MonitorEventChange {

    private static final Logger logger = LoggerFactory.getLogger(MonitorEventChange.class);

    private ApplicationContext context = ApplicationContextProvider.getContext();

    private BinlogEventService eventService = context.getBean(BinlogEventService.class);

    @Getter
    private BinaryLogClient client;

    MonitorEventChange() {
        client = newClient();
    }

    void monitorChange() throws Exception {
        logger.info("==========开始监测binlog的变动==========");
        BinlogEventInfo recentEvent = eventService.recentEvent();
        if (recentEvent == null) {
            BinlogInfo info = JdbcUtils.queryBinlog();
            client.setBinlogFilename(info.getFileName());
            client.setBinlogPosition(info.getPosition());
        } else {
            client.setBinlogFilename(recentEvent.getBinlogFileName());
            client.setBinlogPosition(recentEvent.getEndPosition());
        }

        client.registerEventListener(new BinlogEventListener());
        client.connect();
    }

    private BinaryLogClient newClient() {
        return new BinaryLogClient(ConfigConstant.BINLOG_SERVER_ADDRESS, ConfigConstant.BINLOG_SERVER_PORT,
                ConfigConstant.BINLOG_SERVER_USERNAME, ConfigConstant.BINLOG_SERVER_PASSWORD);
    }

}
