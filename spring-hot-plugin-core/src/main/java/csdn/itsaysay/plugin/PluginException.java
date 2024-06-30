package csdn.itsaysay.plugin;


import java.util.function.Supplier;

public class PluginException extends RuntimeException {
    public PluginException() {
        super();
    }

    public PluginException(String message) {
        super(message);
    }

    public PluginException(Throwable cause) {
        super(cause);
    }

    public PluginException(String message, Throwable cause, String... arg) {
        super(String.format(message, arg), cause);
    }

    public static PluginException getPluginException(Throwable throwable, Supplier<PluginException> getException){
        if(throwable instanceof PluginException){
            return (PluginException) throwable;
        }
        return getException.get();
    }
}
