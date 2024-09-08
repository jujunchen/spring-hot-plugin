package vip.aliali.spring.plugin;

import vip.aliali.spring.plugin.register.ClearLoggerCache;
import vip.aliali.spring.plugin.register.RegisterController;
import vip.aliali.spring.plugin.register.RegisterScheduled;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PluginConfiguration {


    @Bean
    public PluginManager createPluginManager(PluginAutoConfiguration configuration, ApplicationContext applicationContext) {
        return new DefaultPluginManager(configuration, applicationContext);
    }

    @Bean
    public RegisterController createRegisterController(ApplicationContext main) {
        return new RegisterController(main);
    }


    @Bean
    public RegisterScheduled createRegisterScheduled(ApplicationContext main) {
        return new RegisterScheduled(main);
    }

    @Bean
    public ClearLoggerCache createClearLoggerCache(ApplicationContext main) {
        return new ClearLoggerCache(main);
    }

}
