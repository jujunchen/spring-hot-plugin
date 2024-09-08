package vip.aliali.spring.plugin.register;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReflectUtil;
import vip.aliali.spring.plugin.PluginAutoConfiguration;
import vip.aliali.spring.plugin.PluginInfo;
import vip.aliali.spring.plugin.util.DeployUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.*;

/**
 * 清理logger缓存
 * @author jujun.chen
 */
@Slf4j
public class ClearLoggerCache extends AbstractRegister {

    private Map<String, Logger> loggerCache;

    public ClearLoggerCache(ApplicationContext main) {
        super(main);
    }

    @Override
    public void unRegister(ApplicationContext plugin, PluginInfo pluginInfo) {
        try {
            ILoggerFactory iLoggerFactory = LoggerFactory.getILoggerFactory();
            if (iLoggerFactory instanceof LoggerContext) {
                LoggerContext loggerContext = (LoggerContext) iLoggerFactory;
                loggerCache = (Map<String, Logger>) ReflectUtil.getFieldValue(loggerContext, "loggerCache");
                clearLoggerCache(pluginInfo);
            }
        } catch (Exception ex) {
            log.error("clear logger cache error", ex);
        }
    }

    private void clearLoggerCache(PluginInfo pluginInfo) {
        Set<String> keysToRemove = new HashSet<>();
        List<String> classesNames = new ArrayList<>(DeployUtils.readClassFile(pluginInfo.getPath()));
        if (CollUtil.isEmpty(classesNames)) {
            return;
        }

        for (String key : loggerCache.keySet()) {
            String nextPackage = getNextPackage(classesNames.get(0));
            if (key.startsWith(nextPackage)) {
                keysToRemove.add(key);
            }
        }

        for (String key : keysToRemove) {
            Logger logger = loggerCache.get(key);
            //从父级开始删除本身引用，删除后，类实例化的时候会重新创建
            Logger parent = (Logger) ReflectUtil.getFieldValue(logger, "parent");
            List<Logger> childrenList = (List)ReflectUtil.getFieldValue(parent, "childrenList");
            if (CollUtil.isNotEmpty(childrenList)) {
                childrenList.remove(logger);
            }
            loggerCache.remove(key);
        }
    }

    /**
     * 从全限定名中获取basePackage的下一个包名
     * @param className 全限定类名
     * @return
     */
    private String getNextPackage(String className) {
        PluginAutoConfiguration pluginConfiguration =  main.getBean(PluginAutoConfiguration.class);
        String basePackage = pluginConfiguration.getBasePackage();
        int startIndex = className.indexOf(basePackage);
        if (startIndex == -1) {
            return "";
        }

        // 从已知字符串的末尾开始查找下一个'.'
        int endIndex = className.indexOf('.', startIndex + basePackage.length() + 1);
        if (endIndex == -1) {
            return basePackage;
        }

        // 截取我们需要的字符串部分
        return className.substring(0, endIndex);
    }


}
