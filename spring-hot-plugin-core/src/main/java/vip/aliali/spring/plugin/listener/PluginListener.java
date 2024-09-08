package vip.aliali.spring.plugin.listener;


import vip.aliali.spring.plugin.PluginInfo;

/**
 * 插件监听器
 * @author jujunChen
 *
 */
public interface PluginListener {


    /**
     * 注册插件成功
     * @param pluginInfo 插件信息
     */
    default void startSuccess(PluginInfo pluginInfo){}


    /**
     * 启动失败
     * @param pluginInfo 插件信息
     * @param throwable 异常信息
     */
    default void startFailure(PluginInfo pluginInfo, Throwable throwable){}

    /**
     * 卸载插件成功
     * @param pluginInfo 插件信息
     */
    default void stopSuccess(PluginInfo pluginInfo){}


    /**
     * 停止失败
     * @param pluginInfo 插件信息
     * @param throwable 异常信息
     */
    default void stopFailure(PluginInfo pluginInfo, Throwable throwable){}
}
