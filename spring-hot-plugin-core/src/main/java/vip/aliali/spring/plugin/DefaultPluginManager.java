package vip.aliali.spring.plugin;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import vip.aliali.spring.plugin.constants.PluginConstants;
import vip.aliali.spring.plugin.constants.PluginState;
import vip.aliali.spring.plugin.constants.RuntimeMode;
import vip.aliali.spring.plugin.listener.DefaultPluginListenerFactory;
import vip.aliali.spring.plugin.loader.JarLauncher;
import vip.aliali.spring.plugin.loader.archive.Archive;
import vip.aliali.spring.plugin.loader.archive.JarFileArchive;
import vip.aliali.spring.plugin.util.DeployUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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

	private List<PluginInfo> loadPluginsFromPath(List<String> pluginPath) throws Exception {
		List<PluginInfo> pluginInfoList = new ArrayList<>();
		for (String path : pluginPath) {
			Path resolvePath = Paths.get(path);
			Set<PluginInfo> pluginInfos = buildPluginInfo(resolvePath);
			pluginInfoList.addAll(pluginInfos);
		}
	    return pluginInfoList;
	}

	private Set<PluginInfo> buildPluginInfo(Path path) throws Exception {
		//开发环境
		if (RuntimeMode.DEV == pluginAutoConfiguration.environment()) {
			return handleDevPlugin(path);
		}

		//生产环境从jar包中读取
		if (RuntimeMode.PROD == pluginAutoConfiguration.environment()) {
			return handleProdPlugin(path);
		}
		return Collections.EMPTY_SET;
	}

	private Set<PluginInfo> handleProdPlugin(Path path) {
		Set<PluginInfo> pluginInfoList = new HashSet<>();
		//获取jar包列表
		List<File> jarFiles =  FileUtil.loopFiles(path, 1,  file -> file.getName().endsWith(PluginConstants.REPACKAGE + PluginConstants.JAR_SUFFIX));
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
				pluginInfo.setPath(jarFile.getPath());
				pluginInfo.setDependenciesPath(urls);
				pluginInfoList.add(pluginInfo);
			} catch (Exception e) {
				throw new PluginException("插件配置读取异常:" + jarFile.getName(), e);
			}
		}
		return pluginInfoList;
	}

	private Set<PluginInfo> handleDevPlugin(Path path) throws Exception {
		Set<PluginInfo> pluginInfoList = new HashSet<>();
		List<File> pomFiles =  FileUtil.loopFiles(path.toString(), file -> PluginConstants.POM.equals(file.getName()));
		for (File file : pomFiles) {
			if (file.toPath().endsWith(PluginConstants.PLUGIN_POM)) continue;
			MavenXpp3Reader reader = new MavenXpp3Reader();
			Model model = reader.read(Files.newInputStream(file.toPath()));
			PluginInfo pluginInfo = PluginInfo.builder().id(model.getArtifactId())
					.version(model.getVersion() == null ? model.getParent().getVersion() : model.getVersion())
					.description(model.getDescription()).build();
			//开发环境重新定义插件路径，需要指定到classes目录
			pluginInfo.setPath(CharSequenceUtil.subBefore(path.toString(), pluginInfo.getId(), false)
					+ File.separator + pluginInfo.getId()
					+ File.separator + PluginConstants.TARGET
					+ File.separator + PluginConstants.CLASSES);
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
		return pluginInfoList;
	}

	private URL[] toUrls(Iterator<Archive> archives) throws Exception {
		List<URL> urls = new ArrayList<>(50);
		while (archives.hasNext()) {
			URL url = archives.next().getUrl();
			if (!DeployUtils.isUseApplicationJar(applicationContext, url)) {
				urls.add(url);
			}
		}
		return urls.toArray(new URL[0]);
	}



	@Override
	public PluginInfo install(MultipartFile file) {
		if (RuntimeMode.PROD != pluginAutoConfiguration.environment()) {
			throw new PluginException("插件安装只适用于生产环境");
		}
		try {
			Set<PluginInfo> pluginInfos = buildPluginInfo(copyToPluginPath(file));
			if (CollUtil.isEmpty(pluginInfos)) {
				throw new PluginException("插件不存在");
			}
			PluginInfo pluginInfo = (PluginInfo) pluginInfos.toArray()[0];
			if (pluginInfoMap.get(pluginInfo.getId()) != null) {
				log.info("已存在同类插件{}，将覆盖安装", pluginInfo.getId());
				stop(pluginInfo);
			}
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
		//win 下无法删除
		FileUtil.del(Paths.get(pluginInfo.getPath()));
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
		try {
			String backupPath = pluginAutoConfiguration.getBackupPath();
			if (CharSequenceUtil.isBlank(backupPath)) {
				return;
			}
			String newName = pluginInfo.getId()
					+ PluginConstants.JAR_File_ACROSS
					+ pluginInfo.getVersion()
					+ PluginConstants.JAR_File_ACROSS
					+ PluginConstants.REPACKAGE
					+ PluginConstants.JAR_SUFFIX;
			String newPath = backupPath + File.separator + newName;
			FileUtil.copyFile(pluginInfo.getPath(), newPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			throw new PluginException("插件[%s]备份失败", e, pluginInfo.getId());
		}
	}

	private Path copyToPluginPath(MultipartFile file) {
		String pluginPath = pluginAutoConfiguration.getPluginPath();
		if (CharSequenceUtil.isBlank(pluginPath)) {
			throw new PluginException("插件目录不存在");
		}
		if (!pluginPath.endsWith(File.separator)) {
			pluginPath += File.separator;
		}
		try {
			File newFile = FileUtil.writeFromStream(file.getInputStream(), pluginPath + file.getOriginalFilename());
			return Paths.get(newFile.getPath());
		} catch (Exception e) {
			throw new PluginException("插件上传失败", e);
		}
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
