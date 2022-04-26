package icu.mmmc.cr;

import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.entities.NodeInfo;
import icu.mmmc.cr.utils.Logger;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.Objects;
import java.util.UUID;

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
    public static synchronized boolean loadIdentity(NodeInfo nodeInfo, PrivateKey privateKey) {
        if (configuration == null) {
            Logger.warn("Cr 未初始化");
            return false;
        } else if (Cr.nodeInfo != null) {
            Logger.warn("不可重复加载身份");
            return false;
        } else if (nodeInfo == null || privateKey == null || nodeInfo.getPublicKey() == null) {
            Logger.warn("身份信息不完整");
            return false;
        } else if (!Objects.equals(nodeInfo.getUuid(), UUID.nameUUIDFromBytes(nodeInfo.getPublicKey().getEncoded()).toString())) {
            Logger.warn("身份信息异常");
            return false;
        }
        Cr.nodeInfo = nodeInfo;
        Cr.privateKey = privateKey;
        return true;
    }

    /**
     * 连接到一个节点
     *
     * @param address  *网络地址
     * @param callback 回调
     */
    public static void connectToNode(InetSocketAddress address, ProgressCallback callback) {
        try {
            NodeManager.connectToNode(address, callback);
        } catch (Exception e) {
            Logger.warn(e);
            if (callback != null) {
                callback.halt(Objects.requireNonNullElse(e.getMessage(), e.toString()));
            }
        }
    }

    /**
     * 初始化
     *
     * @param configuration 配置
     */
    public static synchronized void init(Configuration configuration) throws Exception {
        if (Cr.configuration != null || configuration == null) {
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
        nodeInfo = null;
        privateKey = null;
    }
}
