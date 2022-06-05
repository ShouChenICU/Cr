package icu.mmmc.cr;

import icu.mmmc.cr.callbacks.*;
import icu.mmmc.cr.entities.NodeInfo;
import icu.mmmc.cr.exceptions.IdentityException;
import icu.mmmc.cr.utils.KeyUtils;
import icu.mmmc.cr.utils.Logger;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Cr核心主类
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class Cr {
    private static final Object LOCK = new Object();
    private static Configuration configuration;
    private static NodeInfo nodeInfo;
    private static PrivateKey privateKey;

    /**
     * 初始化并加载身份
     *
     * @param configuration 配置
     */
    public static void init(Configuration configuration, NodeInfo nodeInfo, PrivateKey privateKey) throws Exception {
        synchronized (LOCK) {
            if (Cr.configuration != null || configuration == null) {
                return;
            }
            Logger.info("Cr init");
            Logger.info("Version: " + Version.VERSION_STRING);
            configuration.check();
            if (nodeInfo == null || privateKey == null) {
                throw new IdentityException("Corrupted identity information");
            }
            nodeInfo.check();
            if (!KeyUtils.checkKeyPair(nodeInfo.getPublicKey(), privateKey)) {
                throw new Exception("Key pair mismatch");
            }
            Cr.nodeInfo = nodeInfo;
            Cr.privateKey = privateKey;
            Cr.configuration = configuration;
            Logger.setLevel(configuration.getLogLevel());
            WorkerThreadPool.init(configuration.getWorkerThreadPoolSize());
            ChatRoomManager.loadAllRooms();
            if (configuration.isListen()) {
                NetCore.init(configuration.getListenPort());
            } else {
                NetCore.init(-1);
            }
            Logger.info("Cr init done");
        }
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
     * 终止
     */
    public static void halt() {
        synchronized (LOCK) {
            if (configuration == null) {
                return;
            }
            Logger.info("Cr halt");
            NodeManager.disconnectALL();
            ChatRoomManager.unloadAllRooms();
            NetCore.halt();
            WorkerThreadPool.halt();
            nodeInfo = null;
            privateKey = null;
            CallBack.newConnectionCallback = null;
        }
    }

    /**
     * 节点api接口
     */
    public static class NodeApi {
        /**
         * 连接到一个节点
         *
         * @param address    *网络地址
         * @param expectUUID 期望节点标识码
         * @param callback   回调
         */
        public static void connectToNode(InetSocketAddress address, String expectUUID, ProgressCallback callback) {
            synchronized (LOCK) {
                if (nodeInfo == null) {
                    Logger.warn("Cr not initialized");
                    callback.halt("Cr not initialized");
                    return;
                }
                NodeManager.connectToNode(address, expectUUID, callback);
            }
        }

        /**
         * 断开一个节点的连接
         *
         * @param uuid 节点标识码
         */
        public static void disconnectToNode(String uuid) {
            synchronized (LOCK) {
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
        }

        /**
         * 获取在线节点信息列表
         *
         * @return 在线节点信息列表
         */
        public static List<NodeInfo> getOnlineNodeInfoList() {
            synchronized (LOCK) {
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
        }

        /**
         * 获取在线节点信息列表
         *
         * @return 在线节点信息列表
         */
        public static int getOnlineNodeCount() {
            synchronized (LOCK) {
                if (nodeInfo == null) {
                    Logger.warn("Cr not initialized");
                    return 0;
                }
                return NodeManager.getOnlineNodeCount();
            }
        }

        /**
         * 判断节点是否在线
         *
         * @param uuid 节点标识码
         * @return 连接状态
         */
        public static boolean nodeIsOnline(String uuid) {
            synchronized (LOCK) {
                if (nodeInfo == null) {
                    Logger.warn("Cr not initialized");
                    return false;
                }
                Node node = NodeManager.getByUUID(uuid);
                return node != null && node.isOnline();
            }
        }
    }

    /**
     * 聊天室api接口
     */
    public static class ChatRoomApi {
        /**
         * 创建聊天室
         *
         * @return 创建成功则返回新聊天室，否则返回null
         */
        public static ChatRoom createChatRoom(String title) {
            synchronized (LOCK) {
                if (nodeInfo == null) {
                    Logger.warn("Cr not initialized");
                    return null;
                }
                try {
                    return ChatRoomManager.createChatRoom(nodeInfo.getUUID(), title);
                } catch (Exception e) {
                    Logger.warn(e);
                    return null;
                }
            }
        }

        /**
         * 删除聊天室
         *
         * @return 删除成功返回true，否则返回false
         */
        public static boolean deleteChatRoom(String nodeUUID, String roomUUID) {
            synchronized (LOCK) {
                if (nodeInfo == null) {
                    Logger.warn("Cr not initialized");
                    return false;
                }
                try {
                    ChatRoomManager.deleteChatRoom(nodeUUID, roomUUID);
                    return true;
                } catch (Exception e) {
                    Logger.warn(e);
                    return false;
                }
            }
        }

        /**
         * 获取全部聊天室列表
         *
         * @return 聊天室列表
         */
        public static List<ChatRoom> getAllChatRooms() {
            synchronized (LOCK) {
                if (nodeInfo == null) {
                    Logger.warn("Cr not initialized");
                    return new ArrayList<>();
                }
                return ChatRoomManager.getAllChatRooms();
            }
        }

        /**
         * 获取被管理的房间列表
         *
         * @return 房间列表
         */
        public static List<ChatRoom> getManageRoomList() {
            synchronized (LOCK) {
                if (nodeInfo == null) {
                    Logger.warn("Cr not initialized");
                    return new ArrayList<>();
                }
                return ChatRoomManager.getManageRoomList();
            }
        }

        /**
         * 获取指定节点所管理的房间列表
         *
         * @param nodeUUID 节点标识码
         * @return 房间列表
         */
        static List<ChatRoom> getRoomListByNodeUUID(String nodeUUID) {
            synchronized (LOCK) {
                if (nodeInfo == null) {
                    Logger.warn("Cr not initialized");
                    return new ArrayList<>();
                }
                return ChatRoomManager.getRoomListByNodeUUID(nodeUUID);
            }
        }
    }

    /**
     * 回调设置
     */
    public static class CallBack {
        public static NewConnectionCallback newConnectionCallback;
        public static NodeStatusUpdateCallback nodeStatusUpdateCallback;
        public static ChatRoomUpdateCallback chatRoomUpdateCallback;
        public static JoinNewRoomCallback joinNewRoomCallback;
        public static MsgReceiveCallback msgReceiveCallback;
    }
}
