package csdn.itsaysay.main.plugin.res;

import vip.aliali.spring.plugin.constants.PluginState;
import lombok.Data;

@Data
public class PluginRes {

    /**
     * 插件id
     */
    private String id;

    /**
     * 版本
     */
    private String version;

    /**
     * 描述
     */
    private String description;

    /**
     * 插件启动状态
     */
    private PluginState pluginState;
}
