package csdn.itsaysay.plugin.mybatis;

import cn.hutool.core.util.ReflectUtil;
import csdn.itsaysay.plugin.PluginAutoConfiguration;
import csdn.itsaysay.plugin.PluginInfo;
import csdn.itsaysay.plugin.register.AbstractRegister;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.binding.MapperRegistry;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.beans.FeatureDescriptor;
import java.util.Map;
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
            //注册mapper扫描器
            registerMapperScannerConfigurer(pluginContext);
            //设置mybatis的默认加载器，否则xml中会取不到class，卸载的时候重置成null
            Resources.setDefaultClassLoader(plugin.getClassLoader());
            setMPConfiguration();
        }
    }

    private void registerMapperScannerConfigurer(AnnotationConfigApplicationContext pluginContext) {
        PluginAutoConfiguration pluginAutoConfiguration = main.getBean(PluginAutoConfiguration.class);
        pluginContext.registerBean(MapperScannerConfigurer.class, (bd) -> handleBd(bd, pluginAutoConfiguration.getBasePackage()));
    }

    private void setMPConfiguration() {
        if (main.containsBean("mybatisPlusUtil")) {
            MybatisPlusUtil mybatisPlusUtil = (MybatisPlusUtil) main.getBean("mybatisPlusUtil");
            DefaultSqlSessionFactory sqlSessionFactory = (DefaultSqlSessionFactory) main.getBean("sqlSessionFactory");
            Configuration configuration = sqlSessionFactory.getConfiguration();
            mybatisPlusUtil.setMPConfiguration(configuration);
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
        DefaultSqlSessionFactory sqlSessionFactory = (DefaultSqlSessionFactory) main.getBean("sqlSessionFactory");
        Configuration configuration = sqlSessionFactory.getConfiguration();
        //使用MybatisPlus的时候
        if (main.containsBean("mybatisPlusUtil")) {
            MybatisPlusUtil mybatisPlusUtil = (MybatisPlusUtil) main.getBean("mybatisPlusUtil");
            mybatisPlusUtil.clearMPQuote(configuration, plugin);
        } else {
            MapperRegistry mapperRegistry = configuration.getMapperRegistry();
            //当前插件加载的mapper
            Map<String, MapperFactoryBean> mapperFactoryBeanMap = plugin.getBeansOfType(MapperFactoryBean.class);
            for (MapperFactoryBean mapperFactoryBean : mapperFactoryBeanMap.values()) {
                Class<?> mapper = mapperFactoryBean.getMapperInterface();
                ((Map)ReflectUtil.getFieldValue(mapperRegistry, "knownMappers")).remove(mapper);
            }
        }

        //其他
        //取消默认加载器
        Resources.setDefaultClassLoader(null);
    }


}
