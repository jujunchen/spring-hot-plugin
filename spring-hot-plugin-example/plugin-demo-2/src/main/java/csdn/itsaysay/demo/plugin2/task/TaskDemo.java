package csdn.itsaysay.demo.plugin2.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务
 */
@Slf4j
@Component
public class TaskDemo {

    @Scheduled(cron = "0/1 * * * * ?")
    public void task() {
        log.info("demo2 task starting");
    }
}
