package icu.mmmc.cr;

import icu.mmmc.cr.callbacks.NodeStatusUpdateCallback;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.callbacks.adapters.ProgressAdapter;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.database.DaoManager;
import icu.mmmc.cr.database.interfaces.NodeInfoDao;
import icu.mmmc.cr.entities.NodeInfo;
import icu.mmmc.cr.tasks.InitTask1;
import icu.mmmc.cr.tasks.PushTask;
import icu.mmmc.cr.tasks.SyncRoomTask1;
import icu.mmmc.cr.utils.Logger;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 节点管理器
 * 管理所连接的全部节点
 *
 * @author shouchen
 */
public final class NodeManager {
    /**
     * 连接超时时长 29s
     * 超过此时间即认为连接断开
     */
    private static final long CONNECT_TIMEOUT = TimeUnit.SECONDS.toMillis(29);
    /**
     * 心跳检测周期 17s
     * 每17s检测一次
     */
    private static final long HEART_TEST_CYCLE = TimeUnit.SECONDS.toMillis(17);
    /**
     * 心跳时间 7s
     * 超过这个时间无心跳则测试一次连接
     */
    private static final long HEART_BEAT_TIME = TimeUnit.SECONDS.toMillis(7);
    private static final ScheduledThreadPoolExecutor TIMER_EXECUTOR;
    private static final ConcurrentHashMap<String, Node> NODE_MAP;
    private static final List<Node> CONNECTING_NODE_LIST;

    static {
        TIMER_EXECUTOR = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
        NODE_MAP = new ConcurrentHashMap<>();
        CONNECTING_NODE_LIST = new ArrayList<>();
    }

    /**
     * 连接到一个节点
     *
     * @param address    *网络地址
     * @param expectUUID 期望连接的节点标识码
     * @param callback   回调
     */
    static void connectToNode(InetSocketAddress address, String expectUUID, ProgressCallback callback) {
        if (callback == null) {
            callback = new ProgressAdapter();
        }
        SocketChannel channel = null;
        SelectionKey key;
        try {
            String msg = "Start connect";
            Logger.debug(msg);
            callback.start();
            channel = SocketChannel.open(address);
            msg = "Connecting to " + channel.getRemoteAddress();
            Logger.info(msg);
            callback.update(0, msg);
            channel.configureBlocking(false);
            key = NetCore.register(channel);
            Node node = acceptNode(key, callback);
            node.addTask(new InitTask1(expectUUID, callback));
        } catch (Exception e) {
            Logger.warn(e);
            callback.halt(Objects.requireNonNullElse(e.getMessage(), e.toString()));
            try {
                if (channel != null) {
                    channel.close();
                }
            } catch (Exception e1) {
                Logger.warn(e1);
            }
        }
    }

    /**
     * 接受节点连接
     *
     * @param key 网络键
     */
    static Node acceptNode(SelectionKey key, ProgressCallback callback) throws Exception {
        if (callback == null) {
            callback = new ProgressAdapter();
        }
        ProgressCallback finalCallback = callback;
        Node node = new Node(key) {
            @Override
            public void initDone() {
                synchronized (CONNECTING_NODE_LIST) {
                    CONNECTING_NODE_LIST.remove(this);
                }
                try {
                    String uuid = nodeInfo.getUUID();
                    synchronized (NODE_MAP) {
                        if (NODE_MAP.get(uuid) != null || Objects.equals(Cr.getNodeInfo().getUUID(), uuid)) {
                            String s = "Connect repeatedly " + uuid;
                            finalCallback.halt(s);
                            Logger.debug(s);
                            disconnect();
                        } else {
                            NODE_MAP.put(uuid, this);
                            ChatRoomManager.registerNode(this);
                            heartTest(this);
                            Logger.info("Connected to " + uuid);
                            finalCallback.done();
                            NodeStatusUpdateCallback callback1 = Cr.CallBack.nodeStatusUpdateCallback;
                            if (callback1 != null) {
                                callback1.connected(nodeInfo);
                            }
                            checkTaskTimeOut();
                            // 连接成功后给对方推送自己的完整信息
                            addTask(new PushTask(Cr.getNodeInfo(), null));
                            addTask(new SyncRoomTask1(null));
                        }
                    }
                } catch (Exception e) {
                    Logger.warn(e);
                }
            }

            @Override
            public void initFail() {
                synchronized (CONNECTING_NODE_LIST) {
                    CONNECTING_NODE_LIST.remove(this);
                }
                disconnect();
            }

            @Override
            public void disconnect() {
                String uuid = nodeInfo == null ? null : nodeInfo.getUUID();
                try {
                    super.disconnect();
                } catch (Exception e) {
                    Logger.error(e);
                }
                if (uuid != null) {
                    synchronized (NODE_MAP) {
                        if (NODE_MAP.remove(uuid) != null) {
                            Logger.debug("Remove " + nodeInfo.getUUID());
                            NodeStatusUpdateCallback callback1 = Cr.CallBack.nodeStatusUpdateCallback;
                            if (callback1 != null) {
                                callback1.disconnected(nodeInfo);
                            }
                        }
                    }
                    ChatRoomManager.unregisterNode(uuid);
                }
                Logger.info("Disconnect to " + uuid);
            }
        };
        synchronized (CONNECTING_NODE_LIST) {
            CONNECTING_NODE_LIST.add(node);
        }
        TIMER_EXECUTOR.schedule(() -> {
            if (!node.isOnline()) {
                Logger.debug("Node init time out");
                node.initFail();
            }
        }, CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
        key.attach(node);
        key.interestOps(SelectionKey.OP_READ);
        key.selector().wakeup();
        return node;
    }

    /**
     * 通过标识码获取在线节点
     *
     * @param uuid 唯一标识码
     * @return 节点
     */
    static Node getByUUID(String uuid) {
        return NODE_MAP.get(uuid);
    }

    /**
     * 获取在线节点列表
     *
     * @return 在线节点列表
     */
    static List<Node> getOnlineNodeList() {
        synchronized (NODE_MAP) {
            return new ArrayList<>(NODE_MAP.values());
        }
    }

    /**
     * 获取在线节点数量
     *
     * @return 在线节点数量
     */
    static int getOnlineNodeCount() {
        synchronized (NODE_MAP) {
            return NODE_MAP.size();
        }
    }

    /**
     * 断开所有连接
     */
    static void disconnectALL() {
        synchronized (CONNECTING_NODE_LIST) {
            for (Node node : CONNECTING_NODE_LIST) {
                try {
                    node.disconnect();
                } catch (Exception e) {
                    Logger.warn(e);
                }
            }
            CONNECTING_NODE_LIST.clear();
        }
        for (Node node : NODE_MAP.values()) {
            try {
                node.disconnect();
            } catch (Exception e) {
                Logger.warn(e);
            }
        }
    }

    /**
     * 心跳测试
     *
     * @param node 节点
     */
    static void heartTest(Node node) {
        if (node == null || !node.isConnect()) {
            return;
        }
        long interval = System.currentTimeMillis() - node.getHeartBeat();
        if (interval > CONNECT_TIMEOUT) {
            try {
                node.disconnect();
            } catch (Exception e) {
                Logger.warn(e);
            }
        } else if (interval > HEART_BEAT_TIME) {
            node.postPacket(new PacketBody()
                    .setDestination(0)
                    .setTaskType(TaskTypes.PING));
        }
        TIMER_EXECUTOR.schedule(() -> heartTest(node), HEART_TEST_CYCLE, TimeUnit.MILLISECONDS);
    }

    /**
     * 更新节点信息
     *
     * @param nodeInfo 节点信息
     */
    public static void updateNodeInfo(NodeInfo nodeInfo) {
        Logger.debug("Update node info");
        if (nodeInfo == null) {
            Logger.warn("Node info is null");
            return;
        }
        try {
            nodeInfo.check();
        } catch (Exception e) {
            Logger.warn(e);
            return;
        }
        String uuid = nodeInfo.getUUID();
        NodeInfoDao dao = DaoManager.getNodeInfoDao();
        if (dao != null) {
            // 获取数据库保存的该节点信息
            NodeInfo info = dao.getByUUID(uuid);
            if (info != null) {
                try {
                    info.check();
                    // 如果保存的数据比较新，就不覆盖了
                    if (info.getTimestamp() >= nodeInfo.getTimestamp()) {
                        Logger.debug("Node info is old");
                        return;
                    }
                } catch (Exception ignored) {
                }
            }
            dao.updateNodeInfo(nodeInfo);
        }
        Node node = NODE_MAP.get(uuid);
        if (node != null) {
            node.setNodeInfo(nodeInfo);
        }
        Logger.debug("Update node info complete, uuid = " + uuid);
    }
}
