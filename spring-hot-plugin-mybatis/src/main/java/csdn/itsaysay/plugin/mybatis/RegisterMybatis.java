package csdn.itsaysay.plugin.mybatis;

import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import csdn.itsaysay.plugin.PluginAutoConfiguration;
import csdn.itsaysay.plugin.PluginInfo;
import csdn.itsaysay.plugin.register.AbstractRegister;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
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
        DefaultSqlSessionFactory sqlSessionFactory = (DefaultSqlSessionFactory) main.getBean("sqlSessionFactory");
        Configuration configuration = sqlSessionFactory.getConfiguration();
        if (configuration instanceof MybatisConfiguration) {
            MybatisConfiguration mpConfiguration = (MybatisConfiguration) configuration;
            //关闭mappedStatement的短key缓存生成
            mpConfiguration.setUseGeneratedShortKey(false);
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
        //处理使用MP时候的引用
        if (configuration instanceof MybatisConfiguration) {
            MybatisConfiguration mpConfiguration = (MybatisConfiguration) configuration;
            //当前插件加载的mapper
            Map<String, MapperFactoryBean> mapperFactoryBeanMap = plugin.getBeansOfType(MapperFactoryBean.class);
            for (MapperFactoryBean mapperFactoryBean : mapperFactoryBeanMap.values()) {
                Class<?> mapper = mapperFactoryBean.getMapperInterface();
                //清除MP没有清除掉的引用缓存
                Class<?> modelClass = ReflectionKit.getSuperClassGenericType(mapper, com.baomidou.mybatisplus.core.mapper.Mapper.class, 0);
                TableInfo tableInfo = TableInfoHelper.getTableInfo(modelClass);
                //null 表示已经清理过
                if (tableInfo != null) {
                    //清除Model 的Class引用缓存
                    ((Map)ReflectUtil.getFieldValue(mpConfiguration.getReflectorFactory(), "reflectorMap")).remove(modelClass);
                    ((Map)ReflectUtil.getFieldValue(ReflectionKit.class, "CLASS_FIELD_CACHE")).remove(modelClass);
                    ((Map)ReflectUtil.getFieldValue(TableInfoHelper.class, "TABLE_NAME_INFO_CACHE")).remove(tableInfo.getTableName());
                }
                //删除类型转换处理器中的引用
                ((Map)ReflectUtil.getFieldValue(configuration.getTypeHandlerRegistry(), "typeHandlerMap")).remove(modelClass);
                mpConfiguration.removeMapper(mapper);
            }
            SqlHelper.FACTORY = null;
        }
        //取消默认加载器
        Resources.setDefaultClassLoader(null);
    }


}
