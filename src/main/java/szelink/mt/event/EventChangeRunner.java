package szelink.mt.event;

import lombok.Getter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author mt
 *  开机启动,在EventFixRunner完成之后执行
 *  监测binlog日志文件的变动
 */
@Component
@Configuration
@Order(value = 2)
public class EventChangeRunner implements ApplicationRunner {

    private static MonitorEventChange monitorEventChange;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        monitorEventChange = new MonitorEventChange();
        monitorEventChange.monitorChange();
    }

    public static MonitorEventChange getMonitorEventChange() {
        return monitorEventChange;
    }
}
