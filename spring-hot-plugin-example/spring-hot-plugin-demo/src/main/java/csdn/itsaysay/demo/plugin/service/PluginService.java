package csdn.itsaysay.demo.plugin.service;

import csdn.itsaysay.demo.plugin.req.PluginReq;
import csdn.itsaysay.demo.plugin.res.PluginRes;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PluginService {

	void install(MultipartFile file);

	Boolean start(String pluginId);

	Boolean stop(String pluginId);

    void uninstall(String pluginId);

	List<PluginRes> list(PluginReq req);
}
