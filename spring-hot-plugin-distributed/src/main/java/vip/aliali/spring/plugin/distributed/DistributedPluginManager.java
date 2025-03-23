package vip.aliali.spring.plugin.distributed;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import vip.aliali.spring.plugin.DefaultPluginManager;
import vip.aliali.spring.plugin.PluginAutoConfiguration;
import vip.aliali.spring.plugin.PluginException;
import vip.aliali.spring.plugin.PluginInfo;
import vip.aliali.spring.plugin.constants.RuntimeMode;
import vip.aliali.spring.plugin.distributed.metadata.MetaDataHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author jujun.chen
 */
@Slf4j
public class DistributedPluginManager extends DefaultPluginManager {

    private Map<String, PluginInfo> pluginInfoTmp = new ConcurrentHashMap<>();

    private MetaDataHandler metaDataHandler;


    public DistributedPluginManager(PluginAutoConfiguration pluginAutoConfiguration,
                                    ApplicationContext applicationContext,
                                    MetaDataHandler metaDataHandler) {
        super(pluginAutoConfiguration, applicationContext);
        this.metaDataHandler = metaDataHandler;
    }

    @Override
    public PluginInfo install(MultipartFile file) {
        if (RuntimeMode.PROD != getPluginAutoConfiguration().environment()) {
            throw new PluginException("插件安装只适用于生产环境");
        }
        try {
            Set<PluginInfo> pluginInfos = buildPluginInfo(copyToPluginPath(file));
            if (CollUtil.isEmpty(pluginInfos)) {
                throw new PluginException("插件不存在");
            }
            PluginInfo pluginInfo = (PluginInfo) pluginInfos.toArray()[0];
            pluginInfoTmp.put(pluginInfo.getId(), pluginInfo);
            //push metadata
            metaDataHandler.pushMetaData(pluginInfo);
            return pluginInfo;
        } catch (Exception e) {
            throw new PluginException("插件安装失败", e);
        }
    }
}
