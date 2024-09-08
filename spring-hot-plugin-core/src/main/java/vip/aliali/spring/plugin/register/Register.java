package vip.aliali.spring.plugin.register;

import vip.aliali.spring.plugin.PluginInfo;
import org.springframework.context.ApplicationContext;

/**
 * 功能性bean 注册接口，比如controller接口、定时任务
 */
public interface Register {

    void refreshBeforeRegister(ApplicationContext plugin, PluginInfo pluginInfo);

    void refreshAfterRegister(ApplicationContext plugin, PluginInfo pluginInfo);

    void unRegister(ApplicationContext plugin, PluginInfo pluginInfo);
}
