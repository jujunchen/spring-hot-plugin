package csdn.itsaysay.plugin;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.PathUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import csdn.itsaysay.plugin.constants.PluginConstants;
import csdn.itsaysay.plugin.constants.PluginState;
import csdn.itsaysay.plugin.constants.RuntimeMode;
import csdn.itsaysay.plugin.listener.DefaultPluginListenerFactory;
import csdn.itsaysay.plugin.loader.JarLauncher;
import csdn.itsaysay.plugin.loader.archive.Archive;
import csdn.itsaysay.plugin.loader.archive.JarFileArchive;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

@Slf4j
public class DefaultPluginManager implements PluginManager {


	private PluginAutoConfiguration pluginAutoConfiguration;
	private ApplicationContext applicationContext;
	private DefaultPluginListenerFactory pluginListenerFactory;
	private PluginClassRegister pluginClassRegister;
	private Map<String, ApplicationContext> pluginBeans = new ConcurrentHashMap<>();
	private Map<String, PluginInfo> pluginInfoMap = new ConcurrentHashMap<>();

	private final AtomicBoolean loaded = new AtomicBoolean(false);
	

	public DefaultPluginManager(PluginAutoConfiguration pluginAutoConfiguration,
			ApplicationContext applicationContext) {
		this.pluginAutoConfiguration = pluginAutoConfiguration;
		this.applicationContext = applicationContext;
		this.pluginClassRegister = new PluginClassRegister(applicationContext, pluginAutoConfiguration, pluginBeans);
	}
	
	public void createPluginListenerFactory() {
		this.pluginListenerFactory = new DefaultPluginListenerFactory(applicationContext);
	}

	@Override
	public List<PluginInfo> loadPlugins() throws Exception {
		if(loaded.get()){
			throw new PluginException("不能重复调用: loadPlugins");
		}
		//从配置路径获取插件目录
		//解析插件jar包中的配置，生成配置对象
		List<PluginInfo> pluginInfoList = loadPluginsFromPath(Collections.singletonList(pluginAutoConfiguration.getPluginPath()));
		if (CollectionUtils.isEmpty(pluginInfoList)) {
			log.warn("路径下未发现任何插件");
			return pluginInfoList;
		}
		
		//注册插件
		for (PluginInfo pluginInfo : pluginInfoList) {
			start(pluginInfo);
		}
		loaded.set(true);
		return pluginInfoList;
	}

	private List<PluginInfo> loadPluginsFromPath(List<String> pluginPath) throws IOException, XmlPullParserException {
		List<PluginInfo> pluginInfoList = new ArrayList<>();
		for (String path : pluginPath) {
			Path resolvePath = Paths.get(path);
			Set<PluginInfo> pluginInfos = buildPluginInfo(resolvePath);
			pluginInfoList.addAll(pluginInfos);
		}
	    return pluginInfoList;
	}

	private Set<PluginInfo> buildPluginInfo(Path path) throws IOException, XmlPullParserException {
		Set<PluginInfo> pluginInfoList = new HashSet<>();
		//开发环境
		if (RuntimeMode.DEV == pluginAutoConfiguration.environment()) {
			List<File> pomFiles =  FileUtil.loopFiles(path.toString(), file -> PluginConstants.POM.equals(file.getName()));
			for (File file : pomFiles) {
				if (file.toPath().endsWith(PluginConstants.PLUGIN_POM)) continue;
				MavenXpp3Reader reader = new MavenXpp3Reader();
				Model model = reader.read(Files.newInputStream(file.toPath()));
				PluginInfo pluginInfo = PluginInfo.builder().id(model.getArtifactId())
						.version(model.getVersion() == null ? model.getParent().getVersion() : model.getVersion())
						.description(model.getDescription()).build();
				//开发环境重新定义插件路径，需要指定到classes目录
				pluginInfo.setPath(URLUtil.url(CharSequenceUtil.subBefore(path.toString(), pluginInfo.getId(), false)
						+ File.separator + pluginInfo.getId()
						+ File.separator + PluginConstants.TARGET
						+ File.separator + PluginConstants.CLASSES));
				//lib依赖
				pluginInfo.setDependenciesPath(new URL[]{
						URLUtil.url(CharSequenceUtil.subBefore(path.toString(), pluginInfo.getId(), false)
								+ File.separator + pluginInfo.getId()
								+ File.separator + PluginConstants.TARGET
								+ File.separator + PluginConstants.CLASSES
								+ File.separator + PluginConstants.LIB)
				});
				pluginInfoList.add(pluginInfo);
			}
		}

		//生产环境从jar包中读取
		if (RuntimeMode.PROD == pluginAutoConfiguration.environment()) {
			//获取jar包列表
			List<File> jarFiles =  FileUtil.loopFiles(path.toString(), file -> file.getName().endsWith(PluginConstants.JAR_SUFFIX));
			for (File jarFile : jarFiles) {
				//读取配置
				try(JarFileArchive archive = new JarFileArchive(jarFile)) {
					JarLauncher launcher = new JarLauncher(archive);
					//读取插件中的依赖包
					URL[] urls = toUrls(launcher.getClassPathArchivesIterator());
					Manifest manifest = archive.getManifest();
					Attributes attr = manifest.getMainAttributes();
					PluginInfo pluginInfo = PluginInfo.builder().id(attr.getValue(PluginConstants.PLUGINID))
							.version(attr.getValue(PluginConstants.PLUGINVERSION))
							.description(attr.getValue(PluginConstants.PLUGINDESCRIPTION)).build();
					pluginInfo.setPath(archive.getUrl());
					pluginInfo.setDependenciesPath(urls);
					pluginInfoList.add(pluginInfo);
				} catch (Exception e) {
					throw new PluginException("插件配置读取异常:" + jarFile.getName(), e);
				}
			}
		}
		return pluginInfoList;
	}

	private URL[] toUrls(Iterator<Archive> archives) throws Exception {
		List<URL> urls = new ArrayList<>(50);
		while (archives.hasNext()) {
			urls.add(archives.next().getUrl());
		}
		return urls.toArray(new URL[0]);
	}

	@Override
	public PluginInfo install(Path jarPath) {
		if (RuntimeMode.PROD != pluginAutoConfiguration.environment()) {
			throw new PluginException("插件安装只适用于生产环境");
		}
		try {
			Set<PluginInfo> pluginInfos = buildPluginInfo(jarPath);
			if (CollUtil.isEmpty(pluginInfos)) {
				throw new PluginException("插件不存在");
			}
			PluginInfo pluginInfo = (PluginInfo) pluginInfos.toArray()[0];
			if (pluginInfoMap.get(pluginInfo.getId()) != null) {
				log.info("已存在同类插件{}，将覆盖安装", pluginInfo.getId());
				uninstall(pluginInfo.getId());
			}
            copyToPluginPath(jarPath);
			start(pluginInfo);
			return pluginInfo;
		} catch (Exception e) {
			throw new PluginException("插件安装失败", e);
		}
	}

	private void start(PluginInfo pluginInfo) {
		try {
			pluginClassRegister.register(pluginInfo);
			pluginInfo.setPluginState(PluginState.STARTED);
			pluginInfoMap.put(pluginInfo.getId(), pluginInfo);
			log.info("插件{}启动成功", pluginInfo.getId());
			pluginListenerFactory.startSuccess(pluginInfo);
		} catch (Exception e) {
			pluginListenerFactory.startFailure(pluginInfo, e);
			throw new PluginException("插件[%s]启动异常", e, pluginInfo.getId());
		}
	}

	@Override
	public void uninstall(String pluginId) {
		if (RuntimeMode.PROD != pluginAutoConfiguration.environment()) {
			throw new PluginException("插件卸载只适用于生产环境");
		}
		PluginInfo pluginInfo = pluginInfoMap.get(pluginId);
		if (pluginInfo == null) {
			return;
		}
		stop(pluginInfo);
		backupPlugin(pluginInfo);
		clear(pluginInfo);
	}
	
	@Override
	public PluginInfo start(String pluginId) {
		PluginInfo pluginInfo = pluginInfoMap.get(pluginId);
		start(pluginInfo);
		return pluginInfo;
	}

	@Override
	public PluginInfo stop(String pluginId) {
		PluginInfo pluginInfo = pluginInfoMap.get(pluginId);
		stop(pluginInfo);
		return pluginInfo;
	}

	@Override
	public List<PluginInfo> list(String pluginId) {
		List<PluginInfo> pluginInfoList = new ArrayList<>();
		if (StrUtil.isBlank(pluginId)) {
			return new ArrayList<>(pluginInfoMap.values());
		}
		pluginInfoList.add(pluginInfoMap.get(pluginId));
		return pluginInfoList;
	}

	private void clear(PluginInfo pluginInfo) {
		PathUtil.del(Paths.get(pluginInfo.getPath().getPath()));
		pluginInfoMap.remove(pluginInfo.getId());
	}

	private void stop(PluginInfo pluginInfo) {
		try {
			pluginClassRegister.unRegister(pluginInfo);
			pluginInfo.setPluginState(PluginState.STOPPED);
			pluginListenerFactory.stopSuccess(pluginInfo);
			log.info("插件{}停止成功", pluginInfo.getId());
		} catch (Exception e) {
			throw new PluginException("插件[{}]停止异常", e, pluginInfo.getId());
		}
	}

	private void backupPlugin(PluginInfo pluginInfo) {
		String backupPath = pluginAutoConfiguration.getBackupPath();
		if (CharSequenceUtil.isBlank(backupPath)) {
			return;
		}
		String newName = pluginInfo.getId() + DateUtil.now() + PluginConstants.JAR_SUFFIX;
		String newPath = backupPath + File.separator + newName;
		FileUtil.copyFile(pluginInfo.getPath().getPath(), newPath);
	}

	private void copyToPluginPath(Path jarPath) {
		String pluginPath = pluginAutoConfiguration.getPluginPath();
		if (CharSequenceUtil.isBlank(pluginPath)) {
			throw new PluginException("插件目录不存在");
		}
		FileUtil.copyFile(jarPath.toString(), pluginPath, StandardCopyOption.REPLACE_EXISTING);
	}

	@Override
	public ApplicationContext getApplicationContext(String pluginId) {
		return pluginBeans.get(pluginId);
	}

	@Override
	public List<Object> getBeansWithAnnotation(String pluginId, Class<? extends Annotation> annotationType) {
		ApplicationContext pluginApplicationContext = pluginBeans.get(pluginId);
		if(pluginApplicationContext != null){
			Map<String, Object> beanMap = pluginApplicationContext.getBeansWithAnnotation(annotationType);
			return new ArrayList<>(beanMap.values());
		}
		return new ArrayList<>(0);
	}


	@Override
	public Object getBeanFromApplicationContext(String beanName) {
		for (ApplicationContext context : pluginBeans.values()) {
			try {
				return context.getBean(beanName);
			} catch (Exception ex) {
				//忽略
			}
		}
		return null;
	}
}
