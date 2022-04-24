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
     *
     * @param configuration 配置
     */
    public static synchronized void init(Configuration configuration) throws Exception {
        if (Cr.configuration != null) {
            return;
        }
        Logger.info("Cr init");
        Logger.info("Version: " + Version.VERSION_STRING);
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

    /**
     * 终止
     */
    public static synchronized void halt() {
        if (configuration == null) {
            return;
        }
        Logger.info("Cr stop");
        NetCore.halt();
        WorkerThreadPool.halt();
    }
}
