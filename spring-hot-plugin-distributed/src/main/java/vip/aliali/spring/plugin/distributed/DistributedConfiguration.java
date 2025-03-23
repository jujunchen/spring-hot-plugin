package vip.aliali.spring.plugin.distributed;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import vip.aliali.spring.plugin.PluginAutoConfiguration;
import vip.aliali.spring.plugin.distributed.metadata.MetaDataHandler;

/**
 * @author jujun.chen
 */
@Configuration
public class DistributedConfiguration {


    @Bean
    @Primary
    public DistributedPluginManager createDistributedPluginManager(PluginAutoConfiguration configuration,
                                                                   ApplicationContext applicationContext,
                                                                   MetaDataHandler metaDataHandler) {
        return new DistributedPluginManager(configuration, applicationContext, metaDataHandler);
    }
}
