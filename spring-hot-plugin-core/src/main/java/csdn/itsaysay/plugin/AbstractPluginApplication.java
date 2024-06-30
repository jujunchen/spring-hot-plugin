package csdn.itsaysay.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractPluginApplication {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	private final AtomicBoolean beInitialized = new AtomicBoolean(false);

	public synchronized void initialize(ApplicationContext applicationContext) {
		Objects.requireNonNull(applicationContext, "ApplicationContext can't be null");
        if(beInitialized.get()) {
            throw new RuntimeException("Plugin has been initialized");
        }
		//获取配置
        PluginAutoConfiguration configuration = getConfiguration(applicationContext);
        
        if (Boolean.FALSE.equals(configuration.getEnable())) {
        	log.info("插件已禁用");
        	return;
		}
        
        try {
        	log.info("插件加载环境: {},插件目录: {}", configuration.getRunMode(), String.join(",", configuration.getPluginPath()));
    		
        	DefaultPluginManager pluginManager = getPluginManager(applicationContext);
			pluginManager.createPluginListenerFactory();
			pluginManager.loadPlugins();
			beInitialized.set(true);
			
			log.info("插件启动完成");
		} catch (Exception e) {
			log.error("初始化插件异常", e);
		}
	}
	
	protected PluginAutoConfiguration getConfiguration(ApplicationContext applicationContext) {
		PluginAutoConfiguration configuration = null;
        try {
            configuration = applicationContext.getBean(PluginAutoConfiguration.class);
        } catch (Exception e){
            // no show exception
        }
        if(configuration == null){
            throw new BeanCreationException("没有发现 <PluginAutoConfiguration> Bean");
        }
        return configuration;
    }
	
	protected DefaultPluginManager getPluginManager(ApplicationContext applicationContext) {
		DefaultPluginManager pluginManager = null;
        try {
        	pluginManager = applicationContext.getBean(DefaultPluginManager.class);
        } catch (Exception e){
            // no show exception
        }
        if(pluginManager == null){
            throw new BeanCreationException("没有发现 <DefaultPluginManager> Bean");
        }
        return pluginManager;
    }
}
