package csdn.itsaysay.task.service;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author jujun.chen
 */
@Slf4j
@Component
@EnableScheduling
public class TaskService {

    private static final String TASK_URL = "http://192.168.123.30:8080";

    public static final String INSALL_PLUGIN = "/plugin/install";
    public static final String UNINSTALL_PLUGIN = "/plugin/uninstall";
    public static final String MYBATIS_INSERT = "/mybatis/insert";
    public static final String MYBATIS_QUERY = "/mybatis/getUserXml/%s";
    public static final String MYBATIS_DELETE = "/mybatis/delete/%s";

    //每10分钟执行一次，新增，查询，删除操作
    @Scheduled(cron = "0 0/10 * * * ?")
    public void task() {
        //插件路径
        File file = new File("/home/cjj/spring-hot-plugin-demo/plugin-path/back/plugin-demo-mybatis-1.1-repackage.jar");

        //安装插件
        log.info("安装插件");
        String response = HttpRequest.post(TASK_URL + INSALL_PLUGIN)
                .form("file", file)
                .execute().body();
        log.info("执行1000次，增查删");
        //循环查询1000次
        for (int i = 0; i < 1000; i++) {
            //调用插入数据接口
            String insertId = HttpRequest.get(TASK_URL + MYBATIS_INSERT)
                    .execute().body();
            ThreadUtil.sleep(1000);
            //查询数据
            String query = HttpRequest.get(TASK_URL + String.format(MYBATIS_QUERY, insertId))
                    .execute().body();
            ThreadUtil.sleep(1000);
            //删除数据
            HttpRequest.delete(TASK_URL + String.format(MYBATIS_DELETE, insertId))
                    .execute();
        }

        //卸载插件，参数是pluginId
        log.info("卸载插件");
        HttpRequest.post(TASK_URL + UNINSTALL_PLUGIN)
                .form("pluginId", "plugin-demo-mybatis")
                .execute();
    }
}
