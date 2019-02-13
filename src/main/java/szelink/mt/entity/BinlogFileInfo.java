package szelink.mt.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * @author mt
 * 解析mysql-bin.index后的内容
 */
@Getter
@Setter
public class BinlogFileInfo {


    private String binlogName;

    private Long size;
}
