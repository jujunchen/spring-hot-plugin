package csdn.itsaysay.plugin;

import csdn.itsaysay.plugin.loader.LaunchedURLClassLoader;
import csdn.itsaysay.plugin.loader.jar.JarFile;
import csdn.itsaysay.plugin.register.RegisterController;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.web.context.support.GenericWebApplicationContext;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

/**
 * 插件注册
 */
@Slf4j
public class PluginClassRegister {
	private ApplicationContext applicationContext;
	private PluginAutoConfiguration configuration;
	private Map<String, ApplicationContext> pluginBeans;

	private RegisterController registerController;


	public PluginClassRegister(ApplicationContext applicationContext, PluginAutoConfiguration configuration, Map<String, ApplicationContext> pluginBeans) {
		this.applicationContext = applicationContext;
		this.configuration = configuration;
		this.pluginBeans = pluginBeans;
		this.registerController = new RegisterController(applicationContext);
	}


	public ApplicationContext register(PluginInfo pluginInfo) {
		ApplicationContext pluginApplicationContext =  registerBean(pluginInfo);
		pluginBeans.put(pluginInfo.getId(), pluginApplicationContext);
		return pluginApplicationContext;
	}
	
	public boolean unRegister(PluginInfo pluginInfo) {
		return unRegisterBean(pluginInfo);
	}

	private boolean unRegisterBean(PluginInfo pluginInfo) {
		GenericWebApplicationContext pluginApplicationContext = (GenericWebApplicationContext) pluginBeans.get(pluginInfo.getId());
		pluginApplicationContext.close();
		//取消注册接口
		registerController.unRegister(pluginApplicationContext, pluginInfo);
		//取消注册controller
		pluginBeans.remove(pluginInfo.getId());
		return true;
	}


	private ApplicationContext registerBean(PluginInfo pluginInfo) {
		URLClassLoader classLoader = null;
		try {
			URL[] dependenciesURLs = pluginInfo.getDependenciesPath();
			//插件的类路径加入到自定义classLoader中
			classLoader = createClassLoader(dependenciesURLs);

			//一个插件创建一个applicationContext
			GenericWebApplicationContext pluginApplicationContext = new GenericWebApplicationContext();
			pluginApplicationContext.setResourceLoader(new DefaultResourceLoader(classLoader));
			pluginApplicationContext.setParent(applicationContext);

			ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(pluginApplicationContext);
			scanner.scan(configuration.getBasePackage());
			pluginApplicationContext.refresh();

			//注册接口
			registerController.register(pluginApplicationContext, pluginInfo);

			return pluginApplicationContext;
		} catch (Exception | Error e) {
			throw  new PluginException("注册bean异常", e);
		}
	}

	private URLClassLoader createClassLoader(URL[] newPath) {
		JarFile.registerUrlProtocolHandler();
		return new LaunchedURLClassLoader(newPath, getClass().getClassLoader());
	}

	private void otherSpringBean(GenericWebApplicationContext pluginApplicationContext) {
		//加载其他的bean
		BeanDefinitionRegistry beanDefinitonRegistry = (BeanDefinitionRegistry) pluginApplicationContext.getBeanFactory();
		ScheduledAnnotationBeanPostProcessor scheduledAnnotationBeanPostProcessor = new ScheduledAnnotationBeanPostProcessor();
		BeanDefinitionBuilder usersBeanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(scheduledAnnotationBeanPostProcessor.getClass());
		usersBeanDefinitionBuilder.setScope("singleton");
		beanDefinitonRegistry.registerBeanDefinition("scheduledAnnotationBeanPostProcessor", usersBeanDefinitionBuilder.getRawBeanDefinition());

//		RequestMappingHandlerMapping requestMappingHandlerMapping1 = new RequestMappingHandlerMapping();
//		BeanDefinitionBuilder requestMappingHandlerMappingBeanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(requestMappingHandlerMapping1.getClass());
//		usersBeanDefinitionBuilder.setScope("singleton");
//		beanDefinitonRegistry.registerBeanDefinition("requestMappingHandlerMapping", requestMappingHandlerMappingBeanDefinitionBuilder.getRawBeanDefinition());
	}

	private void handlerMapperBean(ServletWebServerApplicationContext applicationContext,
								   GenericWebApplicationContext pluginApplicationContext,
								   Map<String, Class> mapperNames) {
		mapperNames.forEach((simpleName, clazz) -> {
			SqlSessionTemplate sqlSessionTemplate = (SqlSessionTemplate) applicationContext.getBean("sqlSessionTemplate");
			BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(MapperFactoryBean.class);
			BeanDefinitionRegistry beanFactory = (BeanDefinitionRegistry) pluginApplicationContext.getBeanFactory();
			beanDefinitionBuilder.setScope("singleton");
			beanDefinitionBuilder.addPropertyValue("sqlSessionTemplate", sqlSessionTemplate);
			beanDefinitionBuilder.addPropertyValue("mapperInterface", clazz);
			beanFactory.registerBeanDefinition(simpleName, beanDefinitionBuilder.getRawBeanDefinition());
		});
	}

}
