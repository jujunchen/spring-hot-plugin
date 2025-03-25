package vip.aliali.spring.plugin.distributed.metadata;

import vip.aliali.spring.plugin.PluginInfo;

import java.util.function.Consumer;

/**
 * @author jujun.chen
 */
public interface MetaDataHandler {

    /**
     * 推送元数据变化
     */
    void pushMetaData(PluginInfo pluginInfo, String pluginFlag, Consumer<String> callback);


}
