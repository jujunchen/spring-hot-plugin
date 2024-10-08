package vip.aliali.spring.plugin;

import org.springframework.context.ApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * 插件管理接口
 * @author jujunChen
 *
 */
public interface PluginManager {

	/**
	 * 加载配置目录中全部插件
	 * @return
	 */
	List<PluginInfo> loadPlugins() throws Exception;

	/**
	 * 安装插件,覆盖安装，并备份原版本，只适用于prod环境
	 * @param file 插件输入流
	 * @return
	 */
	PluginInfo install(MultipartFile file);
	
	/**
	 * 卸载插件，停止插件，删除插件，只适用于prod环境
	 * @param pluginId 插件id
	 */
	void uninstall(String pluginId);
	
	/**
	 * 启动插件
	 * @param pluginId 插件id
	 * @return
	 */
	PluginInfo start(String pluginId);

	/**
	 * 停止插件
	 * @param pluginId 插件id
	 * @return
	 */
	PluginInfo stop(String pluginId);

	/**
	 * 获取当前插件列表
	 * @param pluginId 插件id
	 * @return
	 */
	List<PluginInfo> list(String pluginId);

	
	/**
	 * 获取插件加载上下文
	 * @param pluginId
	 * @return
	 */
	ApplicationContext getApplicationContext(String pluginId);

	/**
	 * 从插件上下文中获取指定名称的bean
	 * @param beanName bean名称
	 * @return
	 */
	Object getBeanFromApplicationContext(String beanName);

	/**
	 * 通过注解获取具体插件中的 Bean
	 * @param pluginId 插件id
	 * @param annotationType 指定注解
	 * @return
	 */
	List<Object> getBeansWithAnnotation(String pluginId, Class<? extends Annotation> annotationType);
}
