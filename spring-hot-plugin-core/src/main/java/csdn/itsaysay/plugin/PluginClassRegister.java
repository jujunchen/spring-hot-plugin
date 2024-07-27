package csdn.itsaysay.plugin;

import csdn.itsaysay.plugin.loader.LaunchedURLClassLoader;
import csdn.itsaysay.plugin.loader.jar.JarFile;
import csdn.itsaysay.plugin.util.DeployUtils;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static csdn.itsaysay.plugin.util.DeployUtils.isController;

/**
 * @Author: jujun chen
 * @Description: 插件动态注册
 * @Date: 2017/10/29
 */
@Slf4j
public class PluginClassRegister {
	private ApplicationContext applicationContext;
	private RequestMappingHandlerMapping requestMappingHandlerMapping;
	private Method getMappingForMethod;
	private PluginAutoConfiguration configuration;
	private Map<String, ApplicationContext> pluginBeans;

	private Map<String, Set<RequestMappingInfo>> requestMappings = new ConcurrentHashMap<>();


	public PluginClassRegister(ApplicationContext applicationContext, PluginAutoConfiguration configuration, Map<String, ApplicationContext> pluginBeans) {
		this.applicationContext = applicationContext;
		this.requestMappingHandlerMapping = getRequestMapping();
		this.getMappingForMethod = getRequestMethod();
		this.configuration = configuration;
		this.pluginBeans = pluginBeans;
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
		//取消注册controller
		Set<RequestMappingInfo> requestMappingInfoSet = requestMappings.get(pluginInfo.getId());
		if (requestMappingInfoSet != null) {
			requestMappingInfoSet.forEach(this::unRegisterController);
		}
		requestMappings.remove(pluginInfo.getId());
		pluginBeans.remove(pluginInfo.getId());
		return true;
	}

	private void unRegisterController(RequestMappingInfo requestMappingInfo) {
		requestMappingHandlerMapping.unregisterMapping(requestMappingInfo);
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
			registerControllerToParent(pluginApplicationContext, pluginInfo);

			return pluginApplicationContext;
		} catch (Exception | Error e) {
			throw  new PluginException("注册bean异常", e);
		}
	}

	private URLClassLoader createClassLoader(URL[] newPath) {
		JarFile.registerUrlProtocolHandler();
		return new LaunchedURLClassLoader(newPath, getClass().getClassLoader());
	}

	private void registerControllerToParent(GenericWebApplicationContext pluginApplicationContext,
									   PluginInfo pluginInfo) throws ClassNotFoundException {
		Set<String> classNames = DeployUtils.readClassFile(pluginInfo.getPath().getPath());
		Set<RequestMappingInfo> pluginRequestMappings = new HashSet<>();
		for (String className : classNames) {
			Class<?> aClass = Class.forName(className, false, pluginApplicationContext.getClassLoader());
			if (isController(aClass)) {
				Object bean = pluginApplicationContext.getBean(aClass);
				Set<RequestMappingInfo> requestMappingInfos = registerController(bean);
				printRegisterSuccessController(pluginInfo, requestMappingInfos);
				pluginRequestMappings.addAll(requestMappingInfos);
			}
		}
		requestMappings.put(pluginInfo.getId(), pluginRequestMappings);
	}

	private void printRegisterSuccessController(PluginInfo pluginInfo, Set<RequestMappingInfo> requestMappingInfos) {
		requestMappingInfos.forEach(requestMappingInfo -> log.info("插件{}注册接口{}", pluginInfo.getId(), requestMappingInfo));
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


	private Set<RequestMappingInfo> registerController(Object bean) {
		Set<RequestMappingInfo> requestMappingInfos = new HashSet<>();
		Method[] methods = bean.getClass().getDeclaredMethods();
		for (Method method : methods) {
			if (DeployUtils.isHaveRequestMapping(method)) {
				try {
					RequestMappingInfo requestMappingInfo = (RequestMappingInfo)
							getMappingForMethod.invoke(requestMappingHandlerMapping, method, bean.getClass());
					requestMappingHandlerMapping.registerMapping(requestMappingInfo, bean , method);
					requestMappingInfos.add(requestMappingInfo);
				} catch (Exception e){
					log.error("接口注册异常", e);
				}
			}
		}
		return requestMappingInfos;
	}

	private Method getRequestMethod() {
		try {
			Method method =  ReflectUtils.findDeclaredMethod(requestMappingHandlerMapping.getClass(), "getMappingForMethod", new Class[] { Method.class, Class.class });
			method.setAccessible(true);
			return method;
		} catch (Exception ex) {
			log.error("反射获取detectHandlerMethods异常", ex);
		}
		return null;
	}

	private void appClassLoaderAddURL(URL[] urls) {
		try {
			Object bean = getClass().getClassLoader();
			Method method = ReflectUtils.findDeclaredMethod(bean.getClass(), "addURL", new Class[] { URL.class });
			method.setAccessible(true);
			for (URL url : urls) {
				method.invoke(bean, url);
			}
		} catch (Exception ex) {
			log.error("反射获取Launcher$AppClassLoader异常", ex);
		}
	}

	private RequestMappingHandlerMapping getRequestMapping() {
		return (RequestMappingHandlerMapping) applicationContext.getBean("requestMappingHandlerMapping");
	}


}
