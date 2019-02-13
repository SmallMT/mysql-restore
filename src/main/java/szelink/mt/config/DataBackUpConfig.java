package szelink.mt.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * @author mt
 * mysql 数据备份参数,具体参照application.yml 中的 以data.bak开头的配置
 * 该类用来获取yml文件中关于数据库备份的相关参数
 */
@Configuration
@Component("backUpConfig")
public class DataBackUpConfig {

    @Value("${data.bak.path}")
    @Getter
    @Setter
    private String path;

    @Value("${data.bak.origin-path}")
    @Getter
    @Setter
    private String originPath;

    @Getter
    @Setter
    @Value("${data.bak.num}")
    private int num;

    @Getter
    @Setter
    @Value("${data.bak.backup-cycle}")
    private int backupCycle;

}
