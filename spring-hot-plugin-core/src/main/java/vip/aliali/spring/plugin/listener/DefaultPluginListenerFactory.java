package vip.aliali.spring.plugin.listener;

import vip.aliali.spring.plugin.PluginInfo;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 插件监听工厂
 * @author jujunChen
 *
 */
public class DefaultPluginListenerFactory implements PluginListener {
	private final List<PluginListener> listeners;

    public DefaultPluginListenerFactory(ApplicationContext applicationContext){
        listeners = new ArrayList<>();
        addExtendPluginListener(applicationContext);
    }

    public DefaultPluginListenerFactory(){
        listeners = new ArrayList<>();
    }


    private void addExtendPluginListener(ApplicationContext applicationContext){
    	Map<String, PluginListener> beansOfTypeMap = applicationContext.getBeansOfType(PluginListener.class);
    	if (!beansOfTypeMap.isEmpty()) {
    		listeners.addAll(beansOfTypeMap.values());
		}
    }

    public synchronized void addPluginListener(PluginListener pluginListener) {
        if(pluginListener != null){
            listeners.add(pluginListener);
        }
    }

    public List<PluginListener> getListeners() {
        return listeners;
    }


    @Override
    public void startSuccess(PluginInfo pluginInfo) {
        for (PluginListener listener : listeners) {
            try {
                listener.startSuccess(pluginInfo);
            } catch (Exception e) {
            	
            }
        }
    }

    @Override
    public void startFailure(PluginInfo pluginInfo, Throwable throwable) {
        for (PluginListener listener : listeners) {
            try {
                listener.startFailure(pluginInfo, throwable);
            } catch (Exception e) {
            	
            }
        }
    }

    @Override
    public void stopSuccess(PluginInfo pluginInfo) {
        for (PluginListener listener : listeners) {
            try {
                listener.stopSuccess(pluginInfo);
            } catch (Exception e) {
            		
            }
        }
    }

    @Override
    public void stopFailure(PluginInfo pluginInfo, Throwable throwable) {
        for (PluginListener listener : listeners) {
            try {
                listener.stopFailure(pluginInfo, throwable);
            } catch (Exception e) {
            	
            }
        }
    }
}
