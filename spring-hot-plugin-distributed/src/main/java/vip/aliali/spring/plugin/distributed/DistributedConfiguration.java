package vip.aliali.spring.plugin.distributed;

import com.alibaba.nacos.api.config.ConfigService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import vip.aliali.spring.plugin.PluginAutoConfiguration;
import vip.aliali.spring.plugin.distributed.metadata.MetaDataHandler;
import vip.aliali.spring.plugin.distributed.metadata.NacosMetaDataHandler;

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

    @Bean
    @ConditionalOnClass(name = "com.alibaba.nacos.api.config.ConfigService")
    public MetaDataHandler createMetaDataHandler(ConfigService configService) {
        return new NacosMetaDataHandler(configService);
    }
}
