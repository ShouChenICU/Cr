package icu.mmmc.cr;

import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.entities.NodeInfo;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
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

    static {
        READ_WRITE_LOCK = new ReentrantReadWriteLock();
        NODE_MAP = new HashMap<>();
    }

    /**
     * 连接到一个节点
     *
     * @param address  网络地址
     * @param callback 回调
     */
    public static void connect(InetSocketAddress address, ProgressCallback callback) {
        // TODO: 2022/4/25
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
