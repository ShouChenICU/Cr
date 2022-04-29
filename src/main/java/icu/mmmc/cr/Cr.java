package icu.mmmc.cr;

import icu.mmmc.cr.callbacks.NewConnectionCallback;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.entities.NodeInfo;
import icu.mmmc.cr.utils.Logger;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
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
    public static synchronized void loadIdentity(NodeInfo nodeInfo, PrivateKey privateKey) throws Exception {
        if (configuration == null) {
            throw new Exception("Cr 未初始化");
        } else if (Cr.nodeInfo != null) {
            throw new Exception("不可重复加载身份");
        } else if (nodeInfo == null || privateKey == null || nodeInfo.getPublicKey() == null) {
            throw new Exception("身份信息不完整");
        } else if (!Objects.equals(nodeInfo.getUuid(), UUID.nameUUIDFromBytes(nodeInfo.getPublicKey().getEncoded()).toString())) {
            throw new Exception("身份信息异常");
        }
        Cr.nodeInfo = nodeInfo;
        Cr.privateKey = privateKey;
    }

    /**
     * 获取节点信息
     *
     * @return 节点信息
     */
    public static NodeInfo getNodeInfo() {
        return nodeInfo;
    }

    /**
     * 获取RSA私钥
     *
     * @return RSA私钥
     */
    public static PrivateKey getPrivateKey() {
        return privateKey;
    }

    /**
     * 连接到一个节点
     *
     * @param address  *网络地址
     * @param callback 回调
     */
    public static void connectToNode(InetSocketAddress address, ProgressCallback callback) {
        NodeManager.connectToNode(address, callback);
    }

    /**
     * 断开一个节点的连接
     *
     * @param uuid 节点标识码
     */
    public static void disconnectToNode(String uuid) {
        Node node = NodeManager.getByUUID(uuid);
        if (node != null) {
            try {
                node.disconnect();
            } catch (Exception e) {
                Logger.warn(e);
            }
        }
    }

    /**
     * 获取在线节点信息列表
     *
     * @return 在线节点信息列表
     */
    public static List<NodeInfo> getOnlineNodeInfoList() {
        List<Node> nodes = NodeManager.getOnlineNodeList();
        List<NodeInfo> nodeInfos = new ArrayList<>();
        for (Node node : nodes) {
            nodeInfos.add(node.getNodeInfo());
        }
        return nodeInfos;
    }

    /**
     * 判断节点是否在线
     *
     * @param uuid 节点标识码
     * @return 连接状态
     */
    public static boolean nodeIsOnline(String uuid) {
        Node node = NodeManager.getByUUID(uuid);
        return node != null && node.isOnline();
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
        WorkerThreadPool.init(configuration.getWorkerThreadPoolSize());
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
        NodeManager.disconnectALL();
        NetCore.halt();
        WorkerThreadPool.halt();
        nodeInfo = null;
        privateKey = null;
        CallBack.newConnectionCallback = null;
    }

    public static class CallBack {
        public static NewConnectionCallback newConnectionCallback;
    }
}
