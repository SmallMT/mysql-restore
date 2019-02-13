package szelink.mt.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author mt 2018-11-26
 * mysql binlog master status信息
 */
@Getter
@Setter
@ToString
public class BinlogInfo {


    private String fileName;

    private Long position;

}
