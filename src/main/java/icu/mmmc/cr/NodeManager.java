package icu.mmmc.cr;

import icu.mmmc.cr.callbacks.adapters.ProgressAdapter;
import icu.mmmc.cr.callbacks.ProgressCallback;
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
    public static void connectToNode(InetSocketAddress address, ProgressCallback callback) throws Exception {
        if (callback == null) {
            callback = new ProgressAdapter();
        }
        String msg = "start connect";
        Logger.debug(msg);
        callback.start();
        SocketChannel channel = SocketChannel.open(address);
        msg = "connected to " + channel.getRemoteAddress();
        Logger.debug(msg);
        callback.update(0, msg);
        SelectionKey key = NetCore.register(channel);
        // TODO: 2022/4/26  
    }

    /**
     * 接受节点连接
     *
     * @param key 网络键
     */
    public static void acceptNode(SelectionKey key) {
        // TODO: 2022/4/26
    }

    /**
     * 通过标识码获取在线节点
     *
     * @param uuid 唯一标识码
     * @return 节点
     */
    public Node getByUUID(String uuid) {
        READ_WRITE_LOCK.readLock().lock();
        try {
            return NODE_MAP.get(uuid);
        } finally {
            READ_WRITE_LOCK.readLock().unlock();
        }
    }
}
