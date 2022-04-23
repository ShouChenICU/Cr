package icu.mmmc.cr;

import icu.mmmc.cr.utils.Logger;

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
    public static synchronized void init(Configuration configuration) throws Exception {
        if (Cr.configuration != null) {
            return;
        }
        Logger.info("Cr init");
        configuration.check();
        Cr.configuration = configuration;
        Logger.setLevel(configuration.getLogLevel());
        if (configuration.isListen()) {
            NetCore.init(configuration.getListenPort());
        } else {
            NetCore.init(-1);
        }
        Logger.info("Cr init done");
    }
}
