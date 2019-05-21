package szelink.mt.util;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.BinaryLogFileReader;
import com.github.shyiko.mysql.binlog.event.*;
import com.github.shyiko.mysql.binlog.event.deserialization.ChecksumType;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer;
import szelink.mt.constant.ConfigConstant;
import szelink.mt.constant.CustomizeConstant;

import java.io.*;

/**
 * @author mt
 * binlog event 工具类
 */
public final class BinlogEventUtil {

    /**
     * 导出binlog文件中一个完整事务的中所执行了的sql语句,该事务以startPos开头
     * @param binlog binlog 文件
     * @param startPos 事务的起始位置
     * @param exportedFile 导出sql语句的位置
     */
    public static void exportBinlogSql(String binlog, Long startPos, String exportedFile) {
        BinaryLogClient client = newClient();
        client.setBlocking(false);
        client.setBinlogFilename(binlog);
        client.setBinlogPosition(startPos);
        client.registerEventListener(new BinaryLogClient.EventListener() {
            @Override
            public void onEvent(Event event) {
                EventHeaderV4 headerV4 = event.getHeader();
                EventType type = headerV4.getEventType();
                if (type.equals(EventType.QUERY)) {
                    QueryEventData data = event.getData();
                    if (!data.getSql().equalsIgnoreCase(CustomizeConstant.DML_SQL)) {
                        writeSql(data.getSql(), exportedFile);
                        closeClient(client);
                    }
                } else if (type.equals(EventType.ROWS_QUERY)) {
                    RowsQueryEventData data = event.getData();
                    writeSql(data.getQuery(), exportedFile);
                } else if (type.equals(EventType.XID)) {
                    closeClient(client);
                }
            }
        });
        try {
            client.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void writeSql(String sql, String file) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(new File(file),true));
            writer.write(sql);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static BinaryLogClient newClient() {
        return new BinaryLogClient(ConfigConstant.BINLOG_SERVER_ADDRESS, ConfigConstant.BINLOG_SERVER_PORT,
                ConfigConstant.BINLOG_SERVER_USERNAME, ConfigConstant.BINLOG_SERVER_PASSWORD);
    }

    private static void closeClient(BinaryLogClient client) {
        try {
            client.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void exportBinlogSql(String binlog, Long startPos, Long stopPos, String exportedFile) throws IOException {
        EventDeserializer eventDeserializer = new EventDeserializer();
        eventDeserializer.setChecksumType(ChecksumType.CRC32);
        InputStream in = new FileInputStream(new File(binlog));
        BinaryLogFileReader logFileReader = new BinaryLogFileReader(in, eventDeserializer);
        Event event;
        while ((event = logFileReader.readEvent()) != null) {
            EventHeaderV4 headerV4 = event.getHeader();
            if (headerV4.getPosition() < startPos) {
                continue;
            }
            EventType eventType = headerV4.getEventType();
            if (eventType.equals(EventType.QUERY)) {
                QueryEventData data = event.getData();
                if (!data.getSql().equalsIgnoreCase(CustomizeConstant.DML_SQL)) {
                    writeSql(data.getSql(), exportedFile);
                }
            } else if (eventType.equals(EventType.ROWS_QUERY)) {
                RowsQueryEventData data = event.getData();
                writeSql(data.getQuery(),exportedFile);
            }
            if (headerV4.getPosition() > stopPos) {
                break;
            }
        }
        in.close();
    }


    public static void main(String[] args) throws IOException{
//        BinlogEventUtil.exportBinlogSql("D:\\MySql\\data\\data\\mysql-bin.000002", 6252L, 15629271L,"d:\\mysql-bin.000002-6252.sql");
//        BinlogEventUtil.exportBinlogSql("mysql-bin.000002", 120L, "d:\\mysql-bin.000002-6252.sql");
        BinlogEventUtil.exportBinlogSql("mysql-bin.000001", 6578L, "d:\\mysql-bin.000002-6252.sql");
        System.out.println(3766118L+2402912L+2067504L);
    }

}
