package csdn.itsaysay.plugin.mybatis;

import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.context.ApplicationContext;

import java.util.Map;

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
            for (MapperFactoryBean mapperFactoryBean : mapperFactoryBeanMap.values()) {
                Class<?> mapper = mapperFactoryBean.getMapperInterface();
                //清除MP没有清除掉的引用缓存
                Class<?> modelClass = ReflectionKit.getSuperClassGenericType(mapper, com.baomidou.mybatisplus.core.mapper.Mapper.class, 0);
                TableInfo tableInfo = TableInfoHelper.getTableInfo(modelClass);
                //null 表示已经清理过
                if (tableInfo != null) {
                    //清除Model 的Class引用缓存
                    ((Map) ReflectUtil.getFieldValue(mpConfiguration.getReflectorFactory(), "reflectorMap")).remove(modelClass);
                    ((Map)ReflectUtil.getFieldValue(ReflectionKit.class, "CLASS_FIELD_CACHE")).remove(modelClass);
                    ((Map)ReflectUtil.getFieldValue(TableInfoHelper.class, "TABLE_NAME_INFO_CACHE")).remove(tableInfo.getTableName());
                }
                //删除类型转换处理器中的引用
                ((Map)ReflectUtil.getFieldValue(configuration.getTypeHandlerRegistry(), "typeHandlerMap")).remove(modelClass);
                mpConfiguration.removeMapper(mapper);
            }
            SqlHelper.FACTORY = null;
        }
    }
}
