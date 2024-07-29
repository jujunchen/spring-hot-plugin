package csdn.itsaysay.demo.plugin.res;

import csdn.itsaysay.plugin.constants.PluginState;
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
