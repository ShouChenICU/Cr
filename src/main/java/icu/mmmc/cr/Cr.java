package icu.mmmc.cr;

/**
 * Cr核心主类
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class Cr {
    private static Configuration configuration;

    /**
     * 初始化
     */
    private static synchronized void init(Configuration configuration) throws Exception {
        if (Cr.configuration != null) {
            return;
        }
        Cr.configuration = configuration;
        if (configuration.isListen()) {
            NetCore.init(configuration.getListenPort());
        } else {
            NetCore.init(-1);
        }
    }
}
