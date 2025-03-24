package vip.aliali.spring.plugin.distributed.metadata;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import vip.aliali.spring.plugin.PluginInfo;

import java.util.concurrent.Executor;

/**
 * @author jujun.chen
 */
@Slf4j
public class NacosMetaDataHandler implements MetaDataHandler {

    //元数据key
    public static final String PLUGIN_META_DATA_KEY = "pluginMetaData";
    private static final String PLUGIN_GROUP = "PLUGIN_GROUP";

    private final ConfigService configService;

    public NacosMetaDataHandler(ConfigService configService)  {
        this.configService = configService;
        try {
            configService.addListener(PLUGIN_META_DATA_KEY, PLUGIN_GROUP, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }
                @Override
                public void receiveConfigInfo(String configInfo) {
                    metaDataProcess(configInfo);
                }
            });
        } catch (NacosException e) {
            log.warn("Failed to add listener for Nacos");
        }
    }

    @Override
    public void pushMetaData(PluginInfo pluginInfo) {
        String pluginId = pluginInfo.getId();
        try {
            // 获取现有的pluginMetaData内容
            String existingMetaData = configService.getConfig(PLUGIN_META_DATA_KEY, PLUGIN_GROUP, 5000);
            if (existingMetaData == null || existingMetaData.isEmpty()) {
                // 如果不存在，则创建一个新的配置
                configService.publishConfig(PLUGIN_META_DATA_KEY, PLUGIN_GROUP, pluginId);
            } else {
                // 如果存在，则追加新的插件ID
                String updatedMetaData = existingMetaData + "," + pluginId;
                configService.publishConfig(PLUGIN_META_DATA_KEY, PLUGIN_GROUP, updatedMetaData);
            }
        } catch (NacosException e) {
            log.warn("Plugin Id:{}, Failed to push meta data to Nacos", pluginId);
        }
    }

    @Override
    public void metaDataProcess(String pluginId) {
        log.info("Plugin Id:{}, meta data process", pluginId);
    }
}
