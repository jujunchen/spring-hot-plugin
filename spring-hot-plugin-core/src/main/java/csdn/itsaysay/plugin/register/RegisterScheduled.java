package csdn.itsaysay.plugin.register;

import csdn.itsaysay.plugin.PluginInfo;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

/**
 * 定时任务控制程序
 */
public class RegisterScheduled extends AbstractRegister {

    public RegisterScheduled(ApplicationContext main) {
        super(main);
    }

    @Override
    public void refreshBeforeRegister(ApplicationContext plugin, PluginInfo pluginInfo) {
        if (plugin instanceof AnnotationConfigApplicationContext) {
            ((AnnotationConfigApplicationContext)plugin).registerBean(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME, ScheduledAnnotationBeanPostProcessor.class);
        }
    }

    @Override
    public void unRegister(ApplicationContext plugin, PluginInfo pluginInfo) {
        Object bean = plugin.getBean(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME);
        ((ScheduledAnnotationBeanPostProcessor)bean).destroy();
    }
}
