package icu.mmmc.cr;

import icu.mmmc.cr.callbacks.NewConnectionCallback;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.entities.NodeInfo;
import icu.mmmc.cr.exceptions.IdentityException;
import icu.mmmc.cr.utils.KeyUtils;
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
     * 初始化并加载身份
     *
     * @param configuration 配置
     */
    public static synchronized void init(Configuration configuration, NodeInfo nodeInfo, PrivateKey privateKey) throws Exception {
        if (Cr.configuration != null || configuration == null) {
            return;
        }
        Logger.info("Cr init");
        Logger.info("Version: " + Version.VERSION_STRING);
        configuration.check();
        if (nodeInfo == null || privateKey == null || nodeInfo.getPublicKey() == null) {
            throw new IdentityException("Corrupted identity information");
        } else if (!Objects.equals(nodeInfo.getUuid(), UUID.nameUUIDFromBytes(nodeInfo.getPublicKey().getEncoded()).toString())) {
            throw new IdentityException("UUID error");
        } else if (!KeyUtils.checkKeyPair(nodeInfo.getPublicKey(), privateKey)) {
            throw new Exception("key pair mismatch");
        }
        Cr.nodeInfo = nodeInfo;
        Cr.privateKey = privateKey;
        Cr.configuration = configuration;
        Logger.setLevel(configuration.getLogLevel());
        WorkerThreadPool.init(configuration.getWorkerThreadPoolSize());
        ChatRoomManager.loadAll();
        if (configuration.isListen()) {
            NetCore.init(configuration.getListenPort());
        } else {
            NetCore.init(-1);
        }
        Logger.info("Cr init done");
    }

    /**
     * 获取已加载的节点信息
     *
     * @return 节点信息
     */
    public static synchronized NodeInfo getNodeInfo() {
        return nodeInfo;
    }

    /**
     * 获取已加载的RSA私钥
     *
     * @return RSA私钥
     */
    public static synchronized PrivateKey getPrivateKey() {
        return privateKey;
    }

    /**
     * 连接到一个节点
     *
     * @param address  *网络地址
     * @param callback 回调
     */
    public static synchronized void connectToNode(InetSocketAddress address, ProgressCallback callback) {
        if (nodeInfo == null) {
            Logger.warn("Cr not initialized");
            callback.halt("Cr not initialized");
            return;
        }
        NodeManager.connectToNode(address, callback);
    }

    /**
     * 断开一个节点的连接
     *
     * @param uuid 节点标识码
     */
    public static synchronized void disconnectToNode(String uuid) {
        if (nodeInfo == null) {
            Logger.warn("Cr not initialized");
            return;
        }
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
    public static synchronized List<NodeInfo> getOnlineNodeInfoList() {
        if (nodeInfo == null) {
            Logger.warn("Cr not initialized");
            return new ArrayList<>();
        }
        List<Node> nodes = NodeManager.getOnlineNodeList();
        List<NodeInfo> nodeInfos = new ArrayList<>();
        for (Node node : nodes) {
            nodeInfos.add(node.getNodeInfo());
        }
        return nodeInfos;
    }

    /**
     * 获取聊天室列表
     *
     * @return 聊天室列表
     */
    public static synchronized List<ChatRoom> getChatRoomList() {
        if (nodeInfo == null) {
            Logger.warn("Cr not initialized");
            return new ArrayList<>();
        }
        return ChatRoomManager.getChatRoomList();
    }

    /**
     * 判断节点是否在线
     *
     * @param uuid 节点标识码
     * @return 连接状态
     */
    public static boolean nodeIsOnline(String uuid) {
        if (nodeInfo == null) {
            Logger.warn("Cr not initialized");
            return false;
        }
        Node node = NodeManager.getByUUID(uuid);
        return node != null && node.isOnline();
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
        ChatRoomManager.unloadAll();
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
