package csdn.itsaysay.plugin.register;

import cn.hutool.core.util.ReflectUtil;
import csdn.itsaysay.plugin.PluginInfo;
import csdn.itsaysay.plugin.util.DeployUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * controller 接口注册程序
 */
@Slf4j
public class RegisterController extends AbstractRegister {

    private Map<String, Set<RequestMappingInfo>> requestMappings = new ConcurrentHashMap<>();

    public RegisterController(ApplicationContext main) {
        super(main);
    }
    @Override
    public void refreshAfterRegister(ApplicationContext plugin, PluginInfo pluginInfo) {
        try {
            Set<String> classNames = DeployUtils.readClassFile(pluginInfo.getPath());
            Set<RequestMappingInfo> pluginRequestMappings = new HashSet<>();
            for (String className : classNames) {
                Class<?> aClass = Class.forName(className, false, plugin.getClassLoader());
                if (DeployUtils.isController(aClass)) {
                    Object bean = plugin.getBean(aClass);
                    Set<RequestMappingInfo> requestMappingInfos = registerController(bean);
                    printRegisterSuccessController(pluginInfo, requestMappingInfos);
                    pluginRequestMappings.addAll(requestMappingInfos);
                }
            }
            //注册成功的缓存起来
            requestMappings.put(pluginInfo.getId(), pluginRequestMappings);
        } catch (Exception ex) {
            log.error("register controller error", ex);
        }
    }

    @Override
    public void unRegister(ApplicationContext plugin, PluginInfo pluginInfo) {
		Set<RequestMappingInfo> requestMappingInfoSet = requestMappings.get(pluginInfo.getId());
		if (requestMappingInfoSet != null) {
			requestMappingInfoSet.forEach(this::unRegisterController);
		}
        //删除RequestMappingHandlerAdapter的缓存
        RequestMappingHandlerAdapter requestMappingHandlerAdapter =  main.getBean(RequestMappingHandlerAdapter.class);
        ((HandlerMethodArgumentResolverComposite)ReflectUtil.getFieldValue(requestMappingHandlerAdapter, "argumentResolvers")).clear();
        ((HandlerMethodArgumentResolverComposite)ReflectUtil.getFieldValue(requestMappingHandlerAdapter, "initBinderArgumentResolvers")).clear();

        //销毁单例Bean
        Map<String, Object> controllerBeans = plugin.getBeansWithAnnotation(Controller.class);
        controllerBeans.forEach((name, bean) -> {
            ((Map)ReflectUtil.getFieldValue(requestMappingHandlerAdapter, "sessionAttributesHandlerCache")).remove(bean.getClass());
            ((Map)ReflectUtil.getFieldValue(requestMappingHandlerAdapter, "initBinderCache")).remove(bean.getClass());
            ((Map)ReflectUtil.getFieldValue(requestMappingHandlerAdapter, "modelAttributeCache")).remove(bean.getClass());
            ((DefaultListableBeanFactory)((AnnotationConfigServletWebServerApplicationContext)main).getBeanFactory()).destroySingleton(name);
        });
		requestMappings.remove(pluginInfo.getId());
    }


    private void printRegisterSuccessController(PluginInfo pluginInfo, Set<RequestMappingInfo> requestMappingInfos) {
        requestMappingInfos.forEach(requestMappingInfo -> log.info("插件{}注册接口{}", pluginInfo.getId(), requestMappingInfo));
    }


    private Set<RequestMappingInfo> registerController(Object bean) {
		Set<RequestMappingInfo> requestMappingInfos = new HashSet<>();
		Method[] methods = bean.getClass().getDeclaredMethods();
		for (Method method : methods) {
			if (DeployUtils.isHaveRequestMapping(method)) {
				try {
                    //解释接口参数
					RequestMappingInfo requestMappingInfo = (RequestMappingInfo)
                            Objects.requireNonNull(getMappingForMethod()).invoke(getRequestMappingHandlerMapping(), method, bean.getClass());
                    //注册接口
                    getRequestMappingHandlerMapping().registerMapping(requestMappingInfo, bean , method);
					requestMappingInfos.add(requestMappingInfo);
				} catch (Exception e){
					log.error("接口注册异常", e);
				}
			}
		}
		return requestMappingInfos;
	}

    private void unRegisterController(RequestMappingInfo requestMappingInfo) {
        Map<RequestMappingInfo, HandlerMethod> handlerMethodMap = getRequestMappingHandlerMapping().getHandlerMethods();
        HandlerMethod handlerMethod = handlerMethodMap.get(requestMappingInfo);
        //取消方法Bean的对象引用
        ReflectUtil.setFieldValue(handlerMethod, "bean", new Object());
        ReflectUtil.setFieldValue(handlerMethod, "beanType", Object.class);
        //取消方法的映射
        getRequestMappingHandlerMapping().unregisterMapping(requestMappingInfo);
    }

	private Method getMappingForMethod() {
		try {
			Method method =  ReflectUtils.findDeclaredMethod(getRequestMappingHandlerMapping().getClass(), "getMappingForMethod", new Class[] { Method.class, Class.class });
			method.setAccessible(true);
			return method;
		} catch (Exception ex) {
			log.error("反射获取detectHandlerMethods异常", ex);
		}
		return null;
	}

    private RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return (RequestMappingHandlerMapping) main.getBean("requestMappingHandlerMapping");
    }
}
