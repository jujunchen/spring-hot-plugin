package csdn.itsaysay.plugin.register;

import csdn.itsaysay.plugin.PluginInfo;
import csdn.itsaysay.plugin.util.DeployUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.ReflectUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RegisterController implements Register {

    private ApplicationContext main;

    private Map<String, Set<RequestMappingInfo>> requestMappings = new ConcurrentHashMap<>();

    public RegisterController(ApplicationContext main) {
        this.main = main;
    }
    @Override
    public void register(ApplicationContext plugin, PluginInfo pluginInfo) {
        try {
            Set<String> classNames = DeployUtils.readClassFile(pluginInfo.getPath().getPath());
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
