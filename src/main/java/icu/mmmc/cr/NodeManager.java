package icu.mmmc.cr;

import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.callbacks.adapters.ProgressAdapter;
import icu.mmmc.cr.tasks.InitTask1;
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
 *
 * @author shouchen
 */
final class NodeManager {
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
     * @param address  *网络地址
     * @param callback 回调
     */
    public static void connectToNode(InetSocketAddress address, ProgressCallback callback) {
        if (callback == null) {
            callback = new ProgressAdapter();
        }
        SocketChannel channel = null;
        SelectionKey key;
        try {
            String msg = "start connect";
            Logger.debug(msg);
            callback.start();
            channel = SocketChannel.open(address);
            msg = "connecting to " + channel.getRemoteAddress();
            Logger.debug(msg);
            callback.update(0, msg);
            channel.configureBlocking(false);
            key = NetCore.register(channel);
            Node node = acceptNode(key, callback);
            node.addTask(new InitTask1(callback));
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
    public static Node acceptNode(SelectionKey key, ProgressCallback callback) throws Exception {
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
                    String uuid = nodeInfo.getUuid();
                    synchronized (NODE_MAP) {
                        if (NODE_MAP.get(uuid) != null) {
                            finalCallback.halt("connect repeatedly ");
                            Logger.debug("connect repeatedly " + uuid);
                            disconnect();
                        } else {
                            NODE_MAP.put(uuid, this);
                            finalCallback.done();
                            Logger.info("connected to " + uuid);
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
                Logger.debug("disconnect");
                try {
                    super.disconnect();
                } catch (Exception e) {
                    Logger.error(e);
                }
                try {
                    if (isOnline() && NODE_MAP.remove(nodeInfo.getUuid()) != null) {
                        Logger.debug("remove " + nodeInfo.getUuid());
                    }
                } catch (Exception e) {
                    Logger.warn(e);
                }
            }
        };
        synchronized (CONNECTING_NODE_LIST) {
            CONNECTING_NODE_LIST.add(node);
        }
        TIMER_EXECUTOR.schedule(() -> {
            if (!node.isOnline()) {
                Logger.debug("node init time out");
                node.initFail();
            }
        }, 30, TimeUnit.SECONDS);
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
    public static Node getByUUID(String uuid) {
        return NODE_MAP.get(uuid);
    }

    /**
     * 获取在线节点列表
     *
     * @return 在线节点列表
     */
    public static List<Node> getOnlineNodeList() {
        return new ArrayList<>(NODE_MAP.values());
    }

    /**
     * 断开所有连接
     */
    public static void disconnectALL() {
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
}
