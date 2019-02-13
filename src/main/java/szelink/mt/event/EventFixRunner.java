package szelink.mt.event;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import szelink.mt.util.CommonUtil;
import szelink.mt.util.TemporaryVariable;

/**
 * @author mt
 * 项目启动后运行("开机启动")
 * 修复在binlog文件中存在,但是在数据库中不存在的event信息
 */
@Component
@Configuration
@Order(value = 1)
public class EventFixRunner implements ApplicationRunner {


    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 产生一个uuid作为备份文件信息的id(
        // 之后备份文件的时候将会采用这个uuid并且此次备份时间期间内容的event中的bakId也将采用这个uuid)
        TemporaryVariable.DATA_BACKUP_ID = CommonUtil.uuid();
        DealWithAccumulation deal = new DealWithAccumulation();
        // 处理堆积的binlog文件
        deal.dealWithAccumulation();
        // 开机备份一次数据库文件
        deal.backAfterStart();
    }

}
