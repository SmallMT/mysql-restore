package szelink.mt.entity;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/**
 * @author mt
 *
 * 经过解析后的mysql binlog 信息
 */
@Getter
@Data
@Entity
@Table(name = "t_event_info")
public class BinlogEventInfo {

    /**
     * 采用32位uuid
     */
    @Id
    @Column(name = "id",length = 32)
    private String id;

    /**
     * 解析的binlog 文件名称
     */
    @Column(name = "binlog_file_name",length = 16)
    private String binlogFileName;

    /**
     * 解析的binlog 的服务器id
     */
    @Column(name = "server_id")
    private Long serverId;

    /**
     * 一段binlog信息产生的时间
     */
    @Column(name = "binlog_date")
    private Date binlogDate;

    /**
     * 一段binlog信息开始位置
     */
    @Column(name = "start_position")
    private Long startPosition;

    /**
     * 一段binlog信息结束位置
     */
    @Column(name = "end_position")
    private Long endPosition;

    /**
     * 一段binlog信息执行了的sql语句
     */
    @Column(name = "sql", length = 1024 * 1024)
    private byte[] sql;

    /**
     * 当前存储的sql语句是否是大sql语句(超出)CustomizeConstant.BIG_SQL_SIZE
     */
    @Column(name = "is_big")
    private boolean bigSql;


    /**
     * 该条记录入库时间(以时间戳的形式存入,为了便于使用时间比较)
     */
    @Column(name = "save_time")
    private Long saveTime = System.currentTimeMillis();

}
