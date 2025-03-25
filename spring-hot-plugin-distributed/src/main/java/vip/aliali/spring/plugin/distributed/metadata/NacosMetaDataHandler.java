package vip.aliali.spring.plugin.distributed.metadata;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import vip.aliali.spring.plugin.PluginInfo;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * @author jujun.chen
 */
@Slf4j
public class NacosMetaDataHandler implements MetaDataHandler {

    //元数据key
    public static final String PLUGIN_META_DATA_KEY = "pluginMetaData";
    private static final String PLUGIN_GROUP = "PLUGIN_GROUP";

    private final ConfigService configService;
    private final ListenerImpl listener = new ListenerImpl();

    public NacosMetaDataHandler(NacosConfigManager nacosConfigManager)  {
        this.configService = nacosConfigManager.getConfigService();
    }

    @Override
    public void pushMetaData(PluginInfo pluginInfo, String pluginFlag, Consumer<String> callback) {
        synchronized (this) {
            String pluginId = pluginInfo.getId();
            try {
                // 获取现有的pluginMetaData内容
                String existingMetaData = configService.getConfigAndSignListener(PLUGIN_META_DATA_KEY, PLUGIN_GROUP, 5000, listener.setCallback(callback));

                String updatedMetaData;
                if (existingMetaData == null || existingMetaData.isEmpty()) {
                    // 如果不存在，则创建一个新的配置
                    updatedMetaData = pluginId.concat(pluginFlag);
                } else {
                    // 如果存在，则追加新的插件ID
                    updatedMetaData = existingMetaData + "," + pluginId.concat(pluginFlag);
                }
                configService.publishConfig(PLUGIN_META_DATA_KEY, PLUGIN_GROUP, updatedMetaData);
            } catch (NacosException e) {
                log.warn("Plugin Id:{}, Failed to push meta data to Nacos", pluginId);
            }
        }
    }

    static class ListenerImpl implements Listener {

        private Consumer<String> callback;


        @Override
        public Executor getExecutor() {
            return null;
        }
        @Override
        public void receiveConfigInfo(String configInfo) {
            log.info("Received updated meta data from Nacos: {}", configInfo);
            if (configInfo != null) {
                callback.accept(configInfo);
            }
        }

        public Listener setCallback(Consumer<String> callback) {
            this.callback = callback;
            return this;
        }

    }

}
