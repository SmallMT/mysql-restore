package szelink.mt.lister;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.event.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationContext;
import szelink.mt.constant.CustomizeConstant;
import szelink.mt.entity.BinlogEventInfo;
import szelink.mt.service.BinlogEventService;
import szelink.mt.util.*;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * @author mt
 * mysql binlog 监听器
 * 它会时刻接受mysql binlog的变动信息
 */
public class BinlogEventListener implements BinaryLogClient.EventListener {

    /** spring 上下文*/
    private ApplicationContext context = ApplicationContextProvider.getContext();

    /** 默认使用的binlog名称*/
    private String binlogFileName = CustomizeConstant.FIRST_BINLOG_NAME;


    private BinlogEventService eventService = context.getBean(BinlogEventService.class);

    private Long needStopPosition;

    private String needStopBinlogName;

    /** 表明该event事务是否需要入库(合法入库,不合法不入库)
     *  用户正常执行的sql event 事务为合法 (所有合法的 sql 语句都是原生sql语句)
     *  数据恢复时执行的sql event 事务为不合法 (所有不合法的sql语句都是经过mysql加密处理后的密文语句且内容以BINLOG开头)
     *  eg. 合法语句:  delete from t_xxx where id = xxx
     *      不合法语句: BINLOG 'WnIPXBMBAAAAPAAAAIIKAAAAAEcAAAAAAAEADWU0c2FmZV9uZXdfZ2QABHRfeHgAAwMPAwI8AAb+p5pr
     * WnIPXCABAAAALgAAALAKAAAAAEcAAAAAAAEAAgAD//gCAAAAATICAAAA3uPLkQ=='
     * */
    private boolean isLegal = true;

    public BinlogEventListener(String needStopBinlogName,Long needStopPosition) {
        this.needStopBinlogName = needStopBinlogName;
        this.needStopPosition = needStopPosition;
    }

    public BinlogEventListener() {}

    @Getter
    @Setter
    private BinlogEventInfo info = new BinlogEventInfo();

    @Override
    public void onEvent(Event event) {
        EventHeaderV4 headerV4 = event.getHeader();
        EventType eventType = headerV4.getEventType();
        if (eventType.equals(EventType.ROTATE)) {
            // 此event会记录解析了哪个binlog文件
            RotateEventData rotateEventData = event.getData();
            binlogFileName = rotateEventData.getBinlogFilename();
            return;
        }
        if (needStopPosition != null && needStopBinlogName != null) {
            // 如果指定了停止的位置,则在指定的位置上停止
            Long currentPos = headerV4.getPosition();
            if (needStopBinlogName.equals(binlogFileName) && needStopPosition.equals(currentPos)) {
                return;
            }
        }
        // 事件组分为两种情况 1.DDL语句事件 2.DML语句事件
        // 1.DDL语句事件仅仅由一个 类型为QUERY 的event组成
        // 2.DML语句事件由5个event组成,依次为 QUERY ROWS_QUERY TABLE_MAP EXT_WRITE_ROWS XID
        //   DML语句事件以QUERY开头,以XID结尾
        // 从mysql binlog的文件中导出日志时,最小的导出只能以组为单位,即一个DDL语句或者一个DML语句
        if (eventType.equals(EventType.QUERY)) {
            // 判断这组事件是DDL语句还是DML语句
            QueryEventData data = event.getData();
            if (data.getSql().equalsIgnoreCase(CustomizeConstant.DML_SQL)) {
                // 表明这是DML语句,并且是这组事件的开始(这组事件共由5个事件组成)
                // 记录了DML语句事件组的开始位置
                info.setId(CommonUtil.uuid());
                info.setBinlogFileName(binlogFileName);
                info.setServerId(headerV4.getServerId());
                info.setBinlogDate(new Date(headerV4.getTimestamp()));
                info.setStartPosition(headerV4.getPosition());
            } else {
                // 这是DDL语句,这组事件仅由一个事件组成
                checkSql(data.getSql());
                if (!isLegal) {
                    return;
                }
                info.setId(CommonUtil.uuid());
                info.setServerId(headerV4.getServerId());
                info.setBinlogFileName(binlogFileName);
                info.setStartPosition(headerV4.getPosition());
                info.setEndPosition(headerV4.getNextPosition());
                info.setBinlogDate(new Date(headerV4.getTimestamp()));
                dealWithSql(info, data.getSql());
                eventService.save(info);
                doAtLast();
                info = new BinlogEventInfo();
                TemporaryVariable.LAST_TIME_EVENT = info;
            }
        } else if (eventType.equals(EventType.ROWS_QUERY)) {
            // 记录了DML语句事件组中 真正执行了的sql语句
            RowsQueryEventData data = event.getData();
            checkSql(data.getQuery());
            if (isLegal) {
                dealWithSql(info, data.getQuery());
            }
        } else if (eventType.equals(EventType.XID)) {
            // 记录了DML语句中事件组中的结束位置
            if (isLegal) {
                info.setEndPosition(headerV4.getNextPosition());
                eventService.save(info);
            }
            doAtLast();
        }
    }


    /**
     * 获取以utf-8编码的字节数组
     * @param sql
     * @return
     */
    private byte[] getSqlBytes(String sql) {
        return sql.getBytes(StandardCharsets.UTF_8);
    }

    private void dealWithSql(BinlogEventInfo info, String sql) {
        byte[] sqlBytes = getSqlBytes(sql);
        if (sqlBytes.length > CustomizeConstant.BIG_SQL_SIZE) {
            // 大型sql语句
            info.setBigSql(true);
            info.setSql(null);
        } else {
            info.setBigSql(false);
            info.setSql(sqlBytes);
        }
    }

    private void checkSql(String sql) {
        isLegal = !sql.startsWith(CustomizeConstant.ILLEAL_SQL);
    }

    private void doAtLast() {
        if (isLegal) {
            // 新增完成后,重置info
            TemporaryVariable.LAST_TIME_EVENT = info;
            // 重置info信息
            info = new BinlogEventInfo();
        }
        // 重置isLegal
        isLegal = true;
    }

}
