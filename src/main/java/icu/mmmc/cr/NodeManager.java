package icu.mmmc.cr;

import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.callbacks.adapters.ProgressAdapter;
import icu.mmmc.cr.utils.Logger;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 节点管理器
 *
 * @author shouchen
 */
final class NodeManager {
    private static final ReadWriteLock READ_WRITE_LOCK;
    private static final Map<String, Node> NODE_MAP;
    private static final List<Node> CONNECTING_NODE_LIST;

    static {
        READ_WRITE_LOCK = new ReentrantReadWriteLock();
        NODE_MAP = new HashMap<>();
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
            msg = "connected to " + channel.getRemoteAddress();
            Logger.debug(msg);
            callback.update(0, msg);
            key = NetCore.register(channel);
            Node node = acceptNode(key, callback);
            // TODO: 2022/4/27
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
    public static Node acceptNode(SelectionKey key, ProgressCallback callback) {
        if (callback == null) {
            callback = new ProgressAdapter();
        }
        ProgressCallback finalCallback = callback;
        Node node = new Node(key) {
            @Override
            void initDone() {
                synchronized (CONNECTING_NODE_LIST) {
                    CONNECTING_NODE_LIST.remove(this);
                }
                READ_WRITE_LOCK.writeLock().lock();
                try {
                    String uuid = nodeInfo.getUuid();
                    if (NODE_MAP.get(uuid) != null) {
                        finalCallback.halt("connect repeatedly ");
                        Logger.debug("connect repeatedly " + uuid);
                        disconnect();
                    } else {
                        NODE_MAP.put(uuid, this);
                        finalCallback.done();
                        Logger.info("connect to " + uuid);
                    }
                } catch (Exception e) {
                    Logger.warn(e);
                } finally {
                    READ_WRITE_LOCK.writeLock().unlock();
                }
            }

            @Override
            void initFail() {
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
                    Logger.warn(e);
                }
                READ_WRITE_LOCK.writeLock().lock();
                try {
                    Logger.debug("remove " + nodeInfo.getUuid());
                    NODE_MAP.remove(nodeInfo.getUuid());
                } catch (Exception e) {
                    Logger.warn(e);
                } finally {
                    READ_WRITE_LOCK.writeLock().unlock();
                }
            }
        };
        synchronized (CONNECTING_NODE_LIST) {
            CONNECTING_NODE_LIST.add(node);
        }
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
        READ_WRITE_LOCK.readLock().lock();
        try {
            return NODE_MAP.get(uuid);
        } finally {
            READ_WRITE_LOCK.readLock().unlock();
        }
    }
}
