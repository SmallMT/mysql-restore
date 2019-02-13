package szelink.mt.util;

import szelink.mt.entity.BinlogEventInfo;

/**
 * @author mt
 * 存储一些系统统一的可改变的临时变量
 */
public final class TemporaryVariable {

    private TemporaryVariable() {}

    /**
     * 此次备份文件时,备份信息的主键id
     */
    public static String DATA_BACKUP_ID = "";

    /**
     * 最近一次入库的mysql binlog event 信息
     */
    public static BinlogEventInfo LAST_TIME_EVENT = null;

    /**
     * 上一次还原数据时,执行sql语句的文件夹
     */
    public static String LAST_TIME_REPAIR_SQL_FILE = null;

}
