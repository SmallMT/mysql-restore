package szelink.mt.job.bak;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;


/**
 * @author  mt
 * 数据库备份定时任务
 */
@Component
@EnableScheduling
public class DataBackUpTask implements SchedulingConfigurer {

    static String DATA_BACKUP_CRON = "0 0 0 1/1 1/1 *";


    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(new DataBackUpRunnable(), new BackUpTrigger());
    }
}
