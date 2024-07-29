package csdn.itsaysay.demo.plugin.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import csdn.itsaysay.demo.plugin.req.PluginReq;
import csdn.itsaysay.demo.plugin.res.PluginRes;
import csdn.itsaysay.demo.plugin.service.PluginService;
import csdn.itsaysay.plugin.PluginAutoConfiguration;
import csdn.itsaysay.plugin.PluginInfo;
import csdn.itsaysay.plugin.PluginManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Paths;
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
		String targetFileName = null;
		try {
			String firstPath = pluginAutoConfiguration.getPluginPath() + "/tmp";
			targetFileName = String.join(File.separator, firstPath, file.getOriginalFilename());
			//写入文件
			FileUtil.writeFromStream(file.getInputStream(), targetFileName);
			//安装
			pluginManager.install(Paths.get(targetFileName));
		} catch (Exception ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		} finally {
			//删除临时文件
			FileUtil.del(targetFileName);
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
