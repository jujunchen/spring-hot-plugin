package vip.aliali.spring.plugin;

import cn.hutool.core.util.ReflectUtil;
import vip.aliali.spring.plugin.loader.LaunchedURLClassLoader;
import vip.aliali.spring.plugin.loader.jar.JarFile;
import vip.aliali.spring.plugin.register.Register;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Vector;

/**
 * 插件注册
 */
@Slf4j
public class PluginClassRegister {
	private ApplicationContext applicationContext;
	private PluginAutoConfiguration configuration;
	private Map<String, ApplicationContext> pluginBeans;

	public PluginClassRegister(ApplicationContext applicationContext, PluginAutoConfiguration configuration, Map<String, ApplicationContext> pluginBeans) {
		this.applicationContext = applicationContext;
		this.configuration = configuration;
		this.pluginBeans = pluginBeans;
	}


	public ApplicationContext register(PluginInfo pluginInfo) {
		ApplicationContext pluginApplicationContext =  registerBean(pluginInfo);
		pluginBeans.put(pluginInfo.getId(), pluginApplicationContext);
		return pluginApplicationContext;
	}
	
	public boolean unRegister(PluginInfo pluginInfo) throws IOException {
		return unRegisterBean(pluginInfo);
	}

	private boolean unRegisterBean(PluginInfo pluginInfo) throws IOException {
		AnnotationConfigApplicationContext pluginApplicationContext = (AnnotationConfigApplicationContext) pluginBeans.get(pluginInfo.getId());
		//取消注册
		applyUnRegister(pluginApplicationContext, pluginInfo);
		clearClassLoader(pluginApplicationContext);
		clearApplicationContext(pluginApplicationContext);
		pluginBeans.remove(pluginInfo.getId());
		//及时回收掉
		System.gc();
		return true;
	}

	/**
	 * 清理applicationContext
	 * @param pluginApplicationContext
	 */
	private void clearApplicationContext(AnnotationConfigApplicationContext pluginApplicationContext) {
		pluginApplicationContext.setClassLoader(null);
		pluginApplicationContext.close();
	}

	/**
	 * 清理classLoader
	 * @param pluginApplicationContext
	 */
	private void clearClassLoader(AnnotationConfigApplicationContext pluginApplicationContext) {
		LaunchedURLClassLoader launchedURLClassLoader = ((LaunchedURLClassLoader)pluginApplicationContext.getClassLoader());
		ProtectionDomain protectionDomain = ((ProtectionDomain) ReflectUtil.getFieldValue(launchedURLClassLoader, "defaultDomain"));
		ReflectUtil.setFieldValue(protectionDomain, "classloader", null);
		((Map)ReflectUtil.getFieldValue(launchedURLClassLoader, "packages")).clear();
		((Map)ReflectUtil.getFieldValue(launchedURLClassLoader, "pdcache")).clear();
		((Vector)ReflectUtil.getFieldValue(launchedURLClassLoader, "classes")).clear();
		launchedURLClassLoader.close();
	}

	private ApplicationContext registerBean(PluginInfo pluginInfo) {
		URLClassLoader classLoader = null;
		try {
			URL[] dependenciesURLs = pluginInfo.getDependenciesPath();
			//插件的类路径加入到自定义classLoader中
			classLoader = createClassLoader(dependenciesURLs);

			//一个插件创建一个applicationContext
			AnnotationConfigApplicationContext pluginApplicationContext = new AnnotationConfigApplicationContext();
			pluginApplicationContext.setClassLoader(classLoader);
			pluginApplicationContext.setParent(applicationContext);
			pluginApplicationContext.scan(configuration.getBasePackage());
			//上下文刷新前的操作
			applyRefreshBeforeRegister(pluginApplicationContext, pluginInfo);
			pluginApplicationContext.refresh();
			//上下文刷新后的操作
			applyRefreshAfterRegister(pluginApplicationContext, pluginInfo);

			return pluginApplicationContext;
		} catch (Exception | Error e) {
			throw  new PluginException("注册bean异常", e);
		}
	}

	private URLClassLoader createClassLoader(URL[] newPath) {
		JarFile.registerUrlProtocolHandler();
		return new LaunchedURLClassLoader(newPath, getClass().getClassLoader());
	}

	private void applyUnRegister(ApplicationContext pluginApplicationContext, PluginInfo pluginInfo) {
		Map<String, Register> registers = applicationContext.getBeansOfType(Register.class);
		registers.forEach((name, register) -> {
			register.unRegister(pluginApplicationContext, pluginInfo);
		});
	}

	private void applyRefreshAfterRegister(ApplicationContext pluginApplicationContext, PluginInfo pluginInfo) {
		Map<String, Register> registers = applicationContext.getBeansOfType(Register.class);
		registers.forEach((name, register) -> {
			register.refreshAfterRegister(pluginApplicationContext, pluginInfo);
		});
	}

	private void applyRefreshBeforeRegister(AnnotationConfigApplicationContext pluginApplicationContext, PluginInfo pluginInfo) {
		Map<String, Register> registers = applicationContext.getBeansOfType(Register.class);
		registers.forEach((name, register) -> {
			register.refreshBeforeRegister(pluginApplicationContext, pluginInfo);
		});
	}

}
