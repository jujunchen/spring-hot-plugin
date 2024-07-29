package csdn.itsaysay.plugin.register;

import csdn.itsaysay.plugin.PluginInfo;
import org.springframework.context.ApplicationContext;

public class AbstractRegister implements Register{
    protected ApplicationContext main;

    public AbstractRegister(ApplicationContext main) {
        this.main = main;
    }

    @Override
    public void refreshBeforeRegister(ApplicationContext plugin, PluginInfo pluginInfo) {

    }

    @Override
    public void refreshAfterRegister(ApplicationContext plugin, PluginInfo pluginInfo) {

    }

    @Override
    public void unRegister(ApplicationContext plugin, PluginInfo pluginInfo) {

    }
}
