package csdn.itsaysay.plugin.mybatis.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import csdn.itsaysay.plugin.constants.PluginConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

//暂时无用
@Slf4j
public class MapperUtils {

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

	public static boolean isMapper(Class<?> beanType) {
		return AnnotatedElementUtils.hasAnnotation(beanType, Mapper.class);
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


}

