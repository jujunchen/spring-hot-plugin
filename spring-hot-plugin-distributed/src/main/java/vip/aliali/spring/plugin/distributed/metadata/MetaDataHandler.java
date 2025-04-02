package vip.aliali.spring.plugin.distributed.metadata;

import vip.aliali.spring.plugin.PluginInfo;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author jujun.chen
 */
public interface MetaDataHandler {

    /**
     * 推送元数据变化
     */
    void pushMetaData(List<PluginInfo> pluginInfoList);

    void setCallback(Consumer<String> callback);

}
