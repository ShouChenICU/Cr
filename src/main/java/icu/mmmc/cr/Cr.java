package icu.mmmc.cr;

import icu.mmmc.cr.entities.NodeInfo;
import icu.mmmc.cr.utils.Logger;

import java.security.PrivateKey;

/**
 * Cr核心主类
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class Cr {
    private static Configuration configuration;
    private static NodeInfo nodeInfo;
    private static PrivateKey privateKey;

    /**
     * 加载身份
     *
     * @param nodeInfo   节点信息
     * @param privateKey 私钥
     */
    public static synchronized void loadIdentity(NodeInfo nodeInfo, PrivateKey privateKey) {
        if (configuration == null) {
            Logger.warn("Cr 未初始化");
            return;
        }
        Cr.nodeInfo = nodeInfo;
        Cr.privateKey = privateKey;
    }

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
        Logger.info("Cr halt");
        NetCore.halt();
        WorkerThreadPool.halt();
    }
}
