package vip.aliali.spring.plugin;

import org.springframework.beans.BeansException;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Import;

@Import(PluginConfiguration.class)
public class DefaultPluginApplication extends AbstractPluginApplication implements ApplicationContextAware, ApplicationListener<ApplicationStartedEvent> {
	
	private ApplicationContext applicationContext;

	//主程序启动后加载插件
	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		super.initialize(applicationContext);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
