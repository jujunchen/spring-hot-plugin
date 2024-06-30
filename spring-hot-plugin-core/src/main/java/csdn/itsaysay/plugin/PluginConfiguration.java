package csdn.itsaysay.plugin;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PluginConfiguration {


    @Bean
    public PluginManager createPluginManager(PluginAutoConfiguration configuration, ApplicationContext applicationContext) {
        return new DefaultPluginManager(configuration, applicationContext);
    }
}
