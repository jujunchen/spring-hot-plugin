package csdn.itsaysay.demo.plugin;

import csdn.itsaysay.demo.plugin.req.PluginReq;
import csdn.itsaysay.demo.plugin.res.PluginRes;
import csdn.itsaysay.demo.plugin.service.PluginService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/plugin")
public class PluginController {

    @Resource
    PluginService pluginService;

    /**
     * 安装插件
     * @param file
     */
    @PostMapping("/install")
    public void install(@RequestParam("file") MultipartFile file) {
        pluginService.install(file);
    }

    /**
     * 卸载插件
     * @param pluginId
     */
    @PostMapping("/uninstall")
    public void uninstall(@RequestParam("pluginId") String pluginId) {
        pluginService.uninstall(pluginId);
    }

    /**
     * 启动插件
     * @param pluginId
     * @return
     */
    @PostMapping("/start")
    public Boolean start(@RequestParam("pluginId")String pluginId) {
        return pluginService.start(pluginId);
    }

    /**
     * 暂停插件
     * @param pluginId
     * @return
     */
    @PostMapping("/stop")
    public Boolean stop(@RequestParam("pluginId")String pluginId) {
        return pluginService.stop(pluginId);
    }

    /**
     * 获取插件列表
     * @param req
     * @return
     */
    @PostMapping("/list")
    public List<PluginRes> list(@RequestBody PluginReq req) {
        return pluginService.list(req);
    }

}
