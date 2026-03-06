package vip.aliali.spring.plugin;

import vip.aliali.spring.plugin.constants.RuntimeMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

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
    @Value("${enable:false}")
    private Boolean enable;
    
    /**
     * 运行模式
     *  开发环境: dev
     *  生产环境: prod
     */
    @Value("${runMode:prod}")
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
    @Value("${basePackage:csdn.itsaysay.plugin}")
    private String basePackage;
    
    /**
     * 插件白名单，允许加载的插件ID列表
     */
    private List<String> allowedPlugins;
    
    /**
     * 是否启用插件签名验证
     */
    @Value("${verifySignature:false}")
    private Boolean verifySignature;
    
    /**
     * 插件签名公钥路径
     */
    private String publicKeyPath;

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
    
    public List<String> getAllowedPlugins() {
        return allowedPlugins;
    }
    
    public void setAllowedPlugins(List<String> allowedPlugins) {
        this.allowedPlugins = allowedPlugins;
    }
    
    public Boolean getVerifySignature() {
        return verifySignature;
    }
    
    public void setVerifySignature(Boolean verifySignature) {
        this.verifySignature = verifySignature;
    }
    
    public String getPublicKeyPath() {
        return publicKeyPath;
    }
    
    public void setPublicKeyPath(String publicKeyPath) {
        this.publicKeyPath = publicKeyPath;
    }
    
    /**
     * 检查插件是否在白名单中
     */
    public boolean isPluginAllowed(String pluginId) {
        // 如果白名单为空，则允许所有插件
        if (allowedPlugins == null || allowedPlugins.isEmpty()) {
            return true;
        }
        return allowedPlugins.contains(pluginId);
    }
}
