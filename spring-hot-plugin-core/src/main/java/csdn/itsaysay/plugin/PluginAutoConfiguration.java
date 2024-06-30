package csdn.itsaysay.plugin;

import csdn.itsaysay.plugin.constants.RuntimeMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 插件自动化配置
 * @author jujunChen
 *
 */
@ConfigurationProperties(prefix = "plugin")
public class PluginAutoConfiguration {

    /**
     * 是否启用插件功能
     */
    @Value("${enable:true}")
    private Boolean enable;
    
    /**
     * 运行模式
     *  开发环境: development、dev
     *  生产/部署 环境: deployment、prod
     */
    @Value("${runMode:dev}")
    private String runMode;
    
    /**
     * 插件的路径
     */
    private String pluginPath;
    
    /**
     * 在卸载插件后, 备份插件的目录
     */
    @Value("${backupPath:backupPlugin}")
    private String backupPath;

    /**
     * 扫描的包路径
     */
    @Value("${basePackage:com.xxx.iot.plugin}")
    private String basePackage;

    public RuntimeMode environment() {
        return RuntimeMode.byName(runMode);
    }

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public String getRunMode() {
        return runMode;
    }

    public void setRunMode(String runMode) {
        this.runMode = runMode;
    }

    public String getPluginPath() {
        return pluginPath;
    }

    public void setPluginPath(String pluginPath) {
        this.pluginPath = pluginPath;
    }

    public String getBackupPath() {
        return backupPath;
    }

    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }
}
