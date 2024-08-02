package csdn.itsaysay.plugin.mybatis;

import csdn.itsaysay.plugin.PluginAutoConfiguration;
import csdn.itsaysay.plugin.PluginInfo;
import csdn.itsaysay.plugin.register.AbstractRegister;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.beans.FeatureDescriptor;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class RegisterMybatis extends AbstractRegister {

    public RegisterMybatis(ApplicationContext main) {
        super(main);
    }

    @Override
    public void refreshBeforeRegister(ApplicationContext plugin, PluginInfo pluginInfo) {
        if (plugin instanceof AnnotationConfigApplicationContext) {
            AnnotationConfigApplicationContext pluginContext = (AnnotationConfigApplicationContext) plugin;
            PluginAutoConfiguration pluginAutoConfiguration = main.getBean(PluginAutoConfiguration.class);
            pluginContext.registerBean(MapperScannerConfigurer.class, (bd) -> handleBd(bd, pluginAutoConfiguration.getBasePackage()));
        }
    }

    @Override
    public void refreshAfterRegister(ApplicationContext plugin, PluginInfo pluginInfo) {

    }

    private void handleBd(BeanDefinition bd, String basePackage) {
        MutablePropertyValues builder = bd.getPropertyValues();
        builder.addPropertyValue("processPropertyPlaceHolders", true);
        builder.addPropertyValue("annotationClass", Mapper.class);
        builder.addPropertyValue("basePackage", basePackage);
        BeanWrapper beanWrapper = new BeanWrapperImpl(MapperScannerConfigurer.class);
        Set<String> propertyNames = (Set) Stream.of(beanWrapper.getPropertyDescriptors()).map(FeatureDescriptor::getName).collect(Collectors.toSet());
        if (propertyNames.contains("lazyInitialization")) {
            builder.addPropertyValue("lazyInitialization", "${mybatis.lazy-initialization:false}");
        }

        if (propertyNames.contains("defaultScope")) {
            builder.addPropertyValue("defaultScope", "${mybatis.mapper-default-scope:}");
        }
    }

    @Override
    public void unRegister(ApplicationContext plugin, PluginInfo pluginInfo) {

    }


}
