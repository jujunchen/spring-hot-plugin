package csdn.itsaysay.main.plugin.service.impl;

import cn.hutool.core.convert.Convert;
import csdn.itsaysay.main.plugin.req.PluginReq;
import csdn.itsaysay.main.plugin.res.PluginRes;
import csdn.itsaysay.main.plugin.service.PluginService;
import csdn.itsaysay.plugin.PluginAutoConfiguration;
import csdn.itsaysay.plugin.PluginInfo;
import csdn.itsaysay.plugin.PluginManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class PluginServiceImpl implements PluginService {
	
	@Resource
	PluginManager pluginManager;
	@Resource
	PluginAutoConfiguration pluginAutoConfiguration;

	@Override
	public void install(MultipartFile file) {
		try {
			String firstPath = pluginAutoConfiguration.getPluginPath() + "/tmp";
			//安装
			pluginManager.install(file);
		} catch (Exception ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}
	}

	@Override
	public void uninstall(String pluginId) {
		pluginManager.uninstall(pluginId);
	}

	@Override
	public List<PluginRes> list(PluginReq req) {
		List<PluginInfo> pluginInfoList = pluginManager.list(req.getPluginId());
		return Convert.toList(PluginRes.class, pluginInfoList);
	}

	@Override
	public Boolean start(String pluginId) {
		return null;
	}

	@Override
	public Boolean stop(String pluginId) {
		return null;
	}

}
