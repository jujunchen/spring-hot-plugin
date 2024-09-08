package vip.aliali.spring.plugin.constants;

/**
 * 插件状态
 * @author jujunChen
 *
 */
public enum PluginState {
	/**
     * 被禁用状态
     */
    DISABLED("DISABLED"),

    /**
     * 启动状态
     */
    STARTED("STARTED"),


    /**
     * 停止状态
     */
    STOPPED("STOPPED");
	
	private final String status;

    PluginState(String status) {
        this.status = status;
    }
}
