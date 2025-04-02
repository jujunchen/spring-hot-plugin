package vip.aliali.spring.plugin.distributed.metadata;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import vip.aliali.spring.plugin.PluginInfo;
import vip.aliali.spring.plugin.constants.PluginConstants;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author jujun.chen
 */
@Slf4j
public class NacosMetaDataHandler implements MetaDataHandler {

    //元数据key
    public static final String PLUGIN_META_DATA_KEY = "pluginMetaData";
    private static final String PLUGIN_GROUP = "PLUGIN_GROUP";
    private final ConfigService configService;

    private volatile Consumer<String> callback = null;

    public NacosMetaDataHandler(NacosConfigManager nacosConfigManager)  {
        this.configService = nacosConfigManager.getConfigService();
    }

    @Override
    public void setCallback(Consumer<String> cbk) {
        try {
            synchronized (this) {
                if (callback == null) {
                    this.callback = cbk;
                    configService.addListener(PLUGIN_META_DATA_KEY, PLUGIN_GROUP, new Listener() {
                        @Override
                        public Executor getExecutor() {
                            return null;
                        }
                        @Override
                        public void receiveConfigInfo(String configInfo) {
                            callback.accept(configInfo);
                        }
                    });
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to set callback for NacosMetaDataHandler: {}", ex.getMessage());
        }
    }

    @Override
    public void pushMetaData(List<PluginInfo> pluginInfoList) {
        try {
            // 获取现有的pluginMetaData内容,pluginId 逗号分割
            String existingMetaData = configService.getConfig(PLUGIN_META_DATA_KEY, PLUGIN_GROUP, 5000);
            // 跟pluginInfoList 中的pluginId 比较，去掉重复的，然后在push 到nacos上，如果existingMetaData不存在，则全部push
            String updatedMetaData = pluginInfoList.stream().map(PluginInfo::getId).collect(Collectors.joining(","));
            updatedMetaData = StrUtil.isEmpty(updatedMetaData) ? PluginConstants.EMPTY : updatedMetaData;

            configService.publishConfig(PLUGIN_META_DATA_KEY, PLUGIN_GROUP, updatedMetaData);
        } catch (NacosException e) {
            log.warn("Failed to push meta data to Nacos: {}", pluginInfoList);
        }
    }


}
