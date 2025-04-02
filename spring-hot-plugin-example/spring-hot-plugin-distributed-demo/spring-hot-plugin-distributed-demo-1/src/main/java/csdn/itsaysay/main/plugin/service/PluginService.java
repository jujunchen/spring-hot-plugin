package csdn.itsaysay.main.plugin.service;

import csdn.itsaysay.main.plugin.req.PluginReq;
import csdn.itsaysay.main.plugin.res.PluginRes;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PluginService {

	void install(MultipartFile file);

	Boolean start(String pluginId);

	Boolean stop(String pluginId);

    void uninstall(String pluginId);

	List<PluginRes> list(PluginReq req);
}
