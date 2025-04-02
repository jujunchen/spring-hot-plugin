package vip.aliali.spring.plugin.distributed;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import vip.aliali.spring.plugin.DefaultPluginManager;
import vip.aliali.spring.plugin.PluginAutoConfiguration;
import vip.aliali.spring.plugin.PluginInfo;
import vip.aliali.spring.plugin.constants.PluginConstants;
import vip.aliali.spring.plugin.distributed.metadata.MetaDataHandler;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author jujun.chen
 */
@Slf4j
public class DistributedPluginManager extends DefaultPluginManager {


    private final MetaDataHandler metaDataHandler;


    public DistributedPluginManager(PluginAutoConfiguration pluginAutoConfiguration,
                                    ApplicationContext applicationContext,
                                    MetaDataHandler metaDataHandler) {
        super(pluginAutoConfiguration, applicationContext);
        this.metaDataHandler = metaDataHandler;
        this.metaDataHandler.setCallback(this::processMetaChange);
    }

    @Override
    public PluginInfo install(MultipartFile file) {
        PluginInfo pluginInfo = super.install(file);
        metaDataHandler.pushMetaData(super.list(null));
        return pluginInfo;
    }

    @Override
    public void uninstall(String pluginId) {
        super.uninstall(pluginId);
        metaDataHandler.pushMetaData(super.list(null));
    }

    private void processMetaChange(String configInfo) {
        log.info("Received metadata change notification: {}", configInfo);
        if (configInfo == null) return;
        List<String> pluginIdList = super.list(null).stream().map(PluginInfo::getId).collect(Collectors.toList());
        List<String> metaDataList = Arrays.asList(configInfo.split(","));
        //根据缓存中的pluginInfoList比较，比缓存中多出来的，表示其他服务新增的插件，该服务需要新安装；比缓存中少的，表示要卸载该服务中的插件
        List<String> needInstallPluginIdList = metaDataList.stream().filter(pluginId -> !configInfo.equals(PluginConstants.EMPTY) && !pluginIdList.contains(pluginId)).collect(Collectors.toList());
        List<String> needUninstallPluginIdList = pluginIdList.stream().filter(pluginId -> !metaDataList.contains(pluginId)).collect(Collectors.toList());
        log.info("Currently installed plugins in the service: {}, metaData: {}, plugin to be installed: {}, plugin to be uninstalled: {}", pluginIdList, configInfo, needInstallPluginIdList, needUninstallPluginIdList);
        installPlugin(needInstallPluginIdList);
        uninstallPlugin(needUninstallPluginIdList);
    }

    private void uninstallPlugin(List<String> needUninstallPluginIdList) {
        if (CollUtil.isEmpty(needUninstallPluginIdList)) return;
        needUninstallPluginIdList.forEach(pluginId -> {
            stop(pluginId);
            getPluginInfoMap().remove(pluginId);
        });
    }

    private void installPlugin(List<String> needInstallPluginIdList) {
        if (CollUtil.isEmpty(needInstallPluginIdList)) return;
        try {
            List<File> jarFiles =  FileUtil.loopFiles(Paths.get(getPluginAutoConfiguration().getPluginPath()), 1, file -> file.getName().endsWith(PluginConstants.REPACKAGE + PluginConstants.JAR_SUFFIX))
                    .stream().filter(
                            file -> needInstallPluginIdList.stream().anyMatch(pluginId -> file.getName().startsWith(pluginId))
                    ).collect(Collectors.toList());
            for (File jarFile : jarFiles) {
                Set<PluginInfo> pluginInfos = buildPluginInfo(jarFile.toPath());
                start((PluginInfo) pluginInfos.toArray()[0]);
            }
        } catch (Exception ex) {
            log.error("Plugin installation failed", ex);
        }
    }
}
