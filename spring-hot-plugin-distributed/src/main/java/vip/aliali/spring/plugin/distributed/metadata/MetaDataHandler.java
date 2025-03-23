package vip.aliali.spring.plugin.distributed.metadata;

import vip.aliali.spring.plugin.PluginInfo;

/**
 * @author jujun.chen
 */
public interface MetaDataHandler {

    /**
     * 推送元数据变化
     */
    void pushMetaData(PluginInfo pluginInfo);

    /**
     * 监听元数据变化
     */
    void metaDataProcess(String pluginId);

}
