package csdn.itsaysay.plugin;

import csdn.itsaysay.plugin.register.RegisterController;
import csdn.itsaysay.plugin.register.RegisterScheduled;
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

}
