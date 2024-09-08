package vip.aliali.spring.plugin.mybatis;

import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisMapperRegistry;
import com.baomidou.mybatisplus.core.mapper.Mapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用来处理MybatisPlus的工具类
 * @author 阿提说说
 */
public class MybatisPlusUtil {


    public void setMPConfiguration(Configuration configuration) {
        if (configuration instanceof MybatisConfiguration) {
            MybatisConfiguration mpConfiguration = (MybatisConfiguration) configuration;
            //关闭mappedStatement的短key缓存生成
            mpConfiguration.setUseGeneratedShortKey(false);
        }
    }

    public void clearMPQuote(Configuration configuration, ApplicationContext plugin) {
        //处理使用MP时候的引用
        if (configuration instanceof MybatisConfiguration) {
            MybatisConfiguration mpConfiguration = (MybatisConfiguration) configuration;
            //当前插件加载的mapper
            Map<String, MapperFactoryBean> mapperFactoryBeanMap = plugin.getBeansOfType(MapperFactoryBean.class);
            Set<String> mapperRegistryCache = GlobalConfigUtils.getGlobalConfig(mpConfiguration).getMapperRegistryCache();
            for (MapperFactoryBean mapperFactoryBean : mapperFactoryBeanMap.values()) {
                Class<?> mapper = mapperFactoryBean.getMapperInterface();

                //清除MP没有清除掉的引用缓存
                Class<?> modelClass = ReflectionKit.getSuperClassGenericType(mapper, Mapper.class, 0);
                TableInfo tableInfo = TableInfoHelper.getTableInfo(modelClass);
                //null 表示已经清理过
                if (tableInfo != null) {
                    //清除Model 的Class引用缓存
                    ((Map) ReflectUtil.getFieldValue(mpConfiguration.getReflectorFactory(), "reflectorMap")).remove(modelClass);
                    ((Map)ReflectUtil.getFieldValue(ReflectionKit.class, "CLASS_FIELD_CACHE")).remove(modelClass);
                    ((Map)ReflectUtil.getFieldValue(TableInfoHelper.class, "TABLE_NAME_INFO_CACHE")).remove(tableInfo.getTableName());
                    //删除类型转换处理器中的引用
                    ((Map)ReflectUtil.getFieldValue(configuration.getTypeHandlerRegistry(), "typeHandlerMap")).remove(modelClass);
                    // 清空实体表信息映射信息
                    TableInfoHelper.remove(modelClass);
                }

                // 清空 Mapper 缓存信息
                final String mapperType = mapper.toString();
                MybatisMapperRegistry mapperRegistry = ((MybatisMapperRegistry) ReflectUtil.getFieldValue(mpConfiguration, "mybatisMapperRegistry"));
                ReflectUtil.invoke(mapperRegistry, "removeMapper", mapper);
                //删除MappedStatement缓存
                Map<String, MappedStatement> mappedStatements =  ((Map) ReflectUtil.getFieldValue(mpConfiguration, "mappedStatements"));
                final String typeKey = mapper.getName() + StringPool.DOT;
                Set<String> mapperSet = mappedStatements.keySet().stream().filter(ms -> ms.startsWith(typeKey)).collect(Collectors.toSet());
                if (!mapperSet.isEmpty()) {
                    mapperSet.forEach(mappedStatements::remove);
                }
                //删除ResultMap缓存
                Map<String, ResultMap> resultMaps = ((Map)ReflectUtil.getFieldValue(mpConfiguration, "resultMaps"));
                Set<String> resultMapSet = resultMaps.keySet().stream().filter(ms -> ms.startsWith(typeKey)).collect(Collectors.toSet());
                if (!resultMapSet.isEmpty()) {
                    resultMapSet.forEach(resultMaps::remove);
                }
                //删除MP的CRUD的Mapper缓存
                mapperRegistryCache.remove(mapperType);

                //重置资源加载标识
                Set<String> loadedResources = ((Set)ReflectUtil.getFieldValue(configuration, "loadedResources"));
                String xmlResource = getXmlResource(mapper.getName());
                loadedResources.remove(xmlResource);
                loadedResources.remove(mapper.toString());
            }

            SqlHelper.FACTORY = null;
        }
    }

    private String getXmlResource(String name) {
        return name.replace('.', '/') + ".xml";
    }
}
