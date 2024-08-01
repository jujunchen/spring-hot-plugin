package csdn.itsaysay.plugin;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableConfigurationProperties(PluginAutoConfiguration.class)
@Import(DefaultPluginApplication.class)
public class PluginStarter {

}
