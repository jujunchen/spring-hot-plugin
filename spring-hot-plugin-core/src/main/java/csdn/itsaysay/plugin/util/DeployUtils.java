package csdn.itsaysay.plugin.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import csdn.itsaysay.plugin.constants.PluginConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static cn.hutool.core.text.CharSequenceUtil.compareVersion;

@Slf4j
public class DeployUtils {

	private static Map<String, String> applicationJarsNameVersionCache = new ConcurrentHashMap<>(50);

	/**
	 * 读取jar包中所有类文件
	 */
	public static Set<String> readJarFile(String jarAddress) {
	    Set<String> classNameSet = new HashSet<>();
	    
	    try(JarFile jarFile = new JarFile(jarAddress)) {
	    	Enumeration<JarEntry> entries = jarFile.entries();//遍历整个jar文件
		    while (entries.hasMoreElements()) {
		        JarEntry jarEntry = entries.nextElement();
		        String name = jarEntry.getName();
				//读取非依赖包的类文件
		        if (!name.startsWith(PluginConstants.LIB) && name.endsWith(PluginConstants.CLASS_SUFFIX)) {
					String className = name.replaceFirst(PluginConstants.CLASSES + "/", "").replace(PluginConstants.CLASS_SUFFIX, "").replaceAll("/", ".");
//					String className = CharSequenceUtil.sub(name, name.lastIndexOf(PluginConstants.JAR_File_DELIMITER), name.indexOf(PluginConstants.CLASS_SUFFIX)).replace(PluginConstants.JAR_File_DELIMITER, "");
					classNameSet.add(className);
		        }
		    }
		} catch (Exception e) {
			log.warn("加载jar包失败", e);
		}
	    return classNameSet;
	}

	public static InputStream readManifestJarFile(File jarAddress) {
		try {
			JarFile jarFile = new JarFile(jarAddress);
			//遍历整个jar文件
			Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				JarEntry jarEntry = entries.nextElement();
				String name = jarEntry.getName();
				if (name.contains(PluginConstants.MANIFEST)) {
					return jarFile.getInputStream(jarEntry);
				}
			}
		} catch (Exception e) {
			log.warn("加载jar包失败", e);
		}
		return null;
	}

	/**
	 * 方法描述 判断class对象是否带有spring的注解
	 */
//	public static boolean isSpringBeanClass(Class<?> cls) {
//	    if (cls == null) {
//	        return false;
//	    }
//	    //是否是接口
//	    if (cls.isInterface()) {
//	        return false;
//	    }
//	    //是否是抽象类
//	    if (Modifier.isAbstract(cls.getModifiers())) {
//	        return false;
//	    }
//	    if (cls.getAnnotation(Component.class) != null) {
//	        return true;
//	    }
//	    if (cls.getAnnotation(Mapper.class) != null) {
//	        return true;
//	    }
//	    if (cls.getAnnotation(Service.class) != null) {
//	        return true;
//	    }
//		if (cls.getAnnotation(RestController.class) != null) {
//			return true;
//		}
//	    return false;
//	}

//	public static boolean isMapper(Class<?> cls) {
//		if (cls == null) {
//			return false;
//		}
//		if (cls.getAnnotation(Mapper.class) != null) {
//			return true;
//		}
//		return false;
//	}

	
	public static boolean isController(Class<?> beanType) {
		return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) ||
				AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class));
	}

	public static boolean isHaveRequestMapping(Method method) {
		return AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class);
	}
	
	/**
	 * 类名首字母小写 作为spring容器beanMap的key
	 */
	public static String transformName(String className) {
	    String tmpstr = className.substring(className.lastIndexOf(".") + 1);
	    return tmpstr.substring(0, 1).toLowerCase() + tmpstr.substring(1);
	}

	/**
	 * 读取class文件
	 * @param path
	 * @return
	 */
	public static Set<String> readClassFile(String path) {
		if (path.endsWith(PluginConstants.JAR_SUFFIX)) {
			return readJarFile(path);
		} else {
			List<File> pomFiles =  FileUtil.loopFiles(path, file -> file.getName().endsWith(PluginConstants.CLASS_SUFFIX));
			Set<String> classNameSet = new HashSet<>();
			for (File file : pomFiles) {
				String name = file.getPath();
				//读取非依赖包的类文件
				if (!name.contains(PluginConstants.CLASSES + File.separator + PluginConstants.LIB)) {
					String className = CharSequenceUtil.subBetween(name, PluginConstants.CLASSES + File.separator, PluginConstants.CLASS_SUFFIX).replace(File.separator, ".");
					classNameSet.add(className);
				}
			}
			return classNameSet;
		}
	}

	/**
	 * 检验插件中的依赖是否已存在主程序中，存在比较版本大小，插件中的版本比主程序大，保留；插件中的版本比主程序小，丢弃，使用主程序版本
	 * @param applicationContext  主程序上下文
	 * @param url 插件依赖地址
	 * @return true 在主程序中存在，false  不存在
	 */
	public static boolean isUseApplicationJar(ApplicationContext applicationContext, URL url) {
		if (!url.getPath().endsWith(PluginConstants.JAR_SUFFIX + PluginConstants.JAR_DELIMITER)) {
			return false;
		}

		try {
			// 检查缓存中是否有版本信息
			if (!applicationJarsNameVersionCache.isEmpty()) {
				return checkAndCacheVersion(url);
			} else {
				// 如果缓存为空，遍历classLoaderURLs并填充缓存
				return populateCacheAndCheckVersion(applicationContext, url);
			}
		} catch (Exception ex) {
			log.warn("依赖包解释发生错误，将不判断重复性", ex);
			return false;
		}
	}

	private static boolean checkAndCacheVersion(URL url) {
		String jarName = getJarName(url);
		String applicationJarVersion = applicationJarsNameVersionCache.get(jarName);
		if (applicationJarVersion != null) {
			String jarVersion = getJarVersion(url);
			//applicationJarVersion >= jarVersion, 使用主程序版本
			return compareVersion(applicationJarVersion, jarVersion) >= 0;
		} else {
			return false;
		}
	}

	private static boolean populateCacheAndCheckVersion(ApplicationContext applicationContext, URL url) {
		String jarName = getJarName(url);
		URLClassLoader classLoader = (URLClassLoader) applicationContext.getClassLoader();
		for (URL classLoaderURL : classLoader.getURLs()) {
			String applicationJarName = getJarName(classLoaderURL);
			if (StrUtil.isNotBlank(applicationJarName)) {
				applicationJarsNameVersionCache.put(applicationJarName, getJarVersion(classLoaderURL));

				if (applicationJarName.equals(jarName)) {
					return compareVersion(applicationJarsNameVersionCache.get(jarName), getJarVersion(url)) >= 0;
				}
			}
		}
		return false;
	}

	private static String getJarName(URL url) {
		String jarName = getJarFullName(url);
		int index = jarName.lastIndexOf(PluginConstants.JAR_File_ACROSS);
		if (index < 0 || !StrUtil.isNumeric(jarName.substring(index + 1).replace(".", ""))) {
			return jarName;
		}
		return jarName.substring(0, index);
	}

	private static String getJarVersion(URL url) {
		String jarName = getJarFullName(url);
		int index = jarName.lastIndexOf(PluginConstants.JAR_File_ACROSS);
		if (index < 0 || !StrUtil.isNumeric(jarName.substring(index + 1).replace(".", ""))) {
			return "0";
		}
		return jarName.substring(index + 1);
	}

	private static String getJarFullName(URL url) {
		if (url == null || url.getPath() == null || url.getPath().isEmpty()) {
			throw new IllegalArgumentException("URL cannot be null or have an empty path.");
		}
		String jarName = url.getPath();
		if (jarName.endsWith(PluginConstants.JAR_DELIMITER)) {
			jarName = jarName.replace(PluginConstants.JAR_DELIMITER, "");
		}
		jarName = jarName.substring(jarName.lastIndexOf(PluginConstants.JAR_File_DELIMITER) + 1).replace(PluginConstants.JAR_SUFFIX, "");
		return jarName;
	}
}

