package csdn.itsaysay.main.plugin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vip.aliali.spring.plugin.PluginInfo;

/**
 * 插件事件监听
 * @author jujun.chen
 */
@Slf4j
@Component
public class PluginListener implements vip.aliali.spring.plugin.listener.PluginListener {

    @Override
    public void startSuccess(PluginInfo pluginInfo) {
        log.info("{}--->启动成功", pluginInfo.getId());
    }

    @Override
    public void startFailure(PluginInfo pluginInfo, Throwable throwable) {
        log.info("{}--->启动失败", pluginInfo.getId());
    }

    @Override
    public void stopSuccess(PluginInfo pluginInfo) {
        log.info("{}--->停止成功", pluginInfo.getId());
    }

    @Override
    public void stopFailure(PluginInfo pluginInfo, Throwable throwable) {
        log.info("{}--->停止失败", pluginInfo.getId());
    }
}
