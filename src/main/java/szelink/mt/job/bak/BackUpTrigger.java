package szelink.mt.job.bak;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.support.CronTrigger;

import java.util.Date;

/**
 * @author mt
 * 定时器动态更换执行时间
 *
 */
public class BackUpTrigger implements Trigger {


    @Override
    public Date nextExecutionTime(TriggerContext triggerContext) {
        CronTrigger cronTrigger = new CronTrigger(DataBackUpTask.DATA_BACKUP_CRON);
        return cronTrigger.nextExecutionTime(triggerContext);
    }
}
