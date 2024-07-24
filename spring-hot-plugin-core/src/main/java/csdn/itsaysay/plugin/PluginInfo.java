package csdn.itsaysay.plugin;

import csdn.itsaysay.plugin.constants.PluginState;
import lombok.Builder;
import lombok.Data;

import java.net.URL;
import java.util.Objects;

/**
 * 插件信息
 * @author jujunChen
 *
 */
@Builder
@Data
public class PluginInfo {

	/**
	 * 插件id
	 */
	private String id;
	
	/**
	 * 版本
	 */
	private String version;
	
	/**
	 * 描述
	 */
	private String description;

	/**
	 * 插件路径
	 */
	private URL path;

	/**
	 * 插件的依赖所在路径
	 */
	private URL[] dependenciesPath;
	
	/**
	 * 插件启动状态
	 */
	private PluginState pluginState;

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PluginInfo other = (PluginInfo) obj;
		return Objects.equals(id, other.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}


}