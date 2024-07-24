package csdn.itsaysay.plugin.constants;


/**
 * 插件运行环境
 * @author jujunChen
 *
 */
public enum  RuntimeMode {

    /**
     * 开发环境
     */
    DEV("dev"),

    /**
     * 生产环境
     */
    PROD("prod");

    private final String mode;

    public static RuntimeMode byName(String model){
        if(DEV.name().equalsIgnoreCase(model)){
            return RuntimeMode.DEV;
        } else {
            return RuntimeMode.PROD;
        }
    }

    RuntimeMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }
}
