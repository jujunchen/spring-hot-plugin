package csdn.itsaysay.plugin;

import csdn.itsaysay.plugin.util.DeployUtils;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
		String path = pluginInfo.getPath().getPath();
		Set<String> classNames = DeployUtils.readClassFile(path);
		URLClassLoader classLoader = null;
		try {
			//class 加载器
			URL[] dependenciesURLs = pluginInfo.getDependenciesPath();
			//插件自身的class加入到自定义classLoader中
			classLoader = new URLClassLoader(new URL[]{pluginInfo.getPath()}, Thread.currentThread().getContextClassLoader());
			//将lib的类路径加入到appClassLoader中
			appClassLoaderAddURL(dependenciesURLs);

			//一个插件创建一个applicationContext
			GenericWebApplicationContext pluginApplicationContext = new GenericWebApplicationContext();
			pluginApplicationContext.setResourceLoader(new DefaultResourceLoader(classLoader));
			pluginApplicationContext.setParent(applicationContext);

			ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(pluginApplicationContext);
			scanner.scan(configuration.getBasePackage());

			//筛选bean
			List<String> controllerNames = new ArrayList<>();
			Map<String, Class> mapperNames = new HashMap<>();
			for (String className : classNames) {
				Class clazz = classLoader.loadClass(className);
				String simpleClassName = DeployUtils.transformName(className);
				//controller接口
				if (DeployUtils.isController(clazz)) {
					controllerNames.add(simpleClassName);
				}
				//mapper接口
				if (DeployUtils.isMapper(clazz)) {
					mapperNames.put(simpleClassName, clazz);
				}
			}
			//处理其他bean，如定时任务
			otherSpringBean(pluginApplicationContext);
			//实例化插件的mapper，依赖主程序sqlSession
			handlerMapperBean((ServletWebServerApplicationContext) applicationContext, pluginApplicationContext, mapperNames);
			//刷新插件上下文
			pluginApplicationContext.refresh();

			//注册接口
			handlerControllerBean(pluginApplicationContext,pluginInfo, controllerNames);

			return pluginApplicationContext;
		} catch (Exception | Error e) {
			throw new PluginException("注册bean异常", e);
		} finally {
			try {
				if (classLoader != null) {
					classLoader.close();
				}
			} catch (IOException e) {
				log.error("classLoader关闭失败", e);
			}
		}
	}

	private void handlerControllerBean(GenericWebApplicationContext pluginApplicationContext,
									   PluginInfo pluginInfo,
									   List<String> controllerNames) {
		Set<RequestMappingInfo> pluginRequestMappings = new HashSet<>();
		for (String beanName : controllerNames) {
			Object bean = pluginApplicationContext.getBean(beanName);
			//注册接口
			Set<RequestMappingInfo> requestMappingInfos = registerController(bean);
			requestMappingInfos.forEach(requestMappingInfo -> {
				log.info("插件{}注册接口{}", pluginInfo.getId(), requestMappingInfo);
			});
			pluginRequestMappings.addAll(requestMappingInfos);
		}
		requestMappings.put(pluginInfo.getId(), pluginRequestMappings);
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
		Class<?> aClass = bean.getClass();
		Set<RequestMappingInfo> requestMappingInfos = new HashSet<>();
		if (Boolean.TRUE.equals(DeployUtils.isController(aClass))) {
			Method[] methods = aClass.getDeclaredMethods();
			for (Method method : methods) {
				if (DeployUtils.isHaveRequestMapping(method)) {
					try {
						RequestMappingInfo requestMappingInfo = (RequestMappingInfo)
								getMappingForMethod.invoke(requestMappingHandlerMapping, method, aClass);
						requestMappingHandlerMapping.registerMapping(requestMappingInfo, bean, method);
						requestMappingInfos.add(requestMappingInfo);
					} catch (Exception e){
						log.error("接口注册异常", e);
					}
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
