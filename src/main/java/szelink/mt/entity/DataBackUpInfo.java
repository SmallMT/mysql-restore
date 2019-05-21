package szelink.mt.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.Date;

/**
 * @author mt
 * mysql数据库备份信息
 * t_backup_info 该表中只存在一条记录,就是最近一次备份文件的信息
 */
@Entity
@Table(name = "t_backup_info")
@Getter
@Setter
@ToString
public class DataBackUpInfo {

    /**
     * 备份信息主键采用32位UUID
     */
    @Id
    @Column(name = "id",length = 32)
    private String id;

    /**
     * 备份文件名称
     */
    @Column(name = "file_name")
    private String fileName;

    /**
     * 备份文件在磁盘中的完整路径
     */
    @Column(name = "file_path")
    private String filePath;

    /**
     * 文件备份时间
     */
    @Column(name = "backup_time")
    private Date backUpTime;

    /**
     * 文件大小
     */
    @Column(name = "size")
    private Long size;

    /**
     * 备份文件对应的binlog文件名
     */
    @Column(name = "binlog_name",length = 16)
    private String binlogFileName;

    /**
     * 备份文件对应的binlog 最后的位置即该条记录的结束位置
     */
    @Column(name = "position")
    private Long position;


}
