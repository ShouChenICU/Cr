package icu.mmmc.cr;

import icu.mmmc.cr.entities.NodeInfo;
import icu.mmmc.cr.utils.Logger;

import java.nio.channels.SelectionKey;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 节点
 * 一个节点就代表一个Cr实例
 *
 * @author shouchen
 */
abstract class Node extends NetNode {
    private final ReentrantLock postLock;
    private final Queue<PacketBody> waitSendPacketQueue;
    protected NodeInfo nodeInfo;
    private Encryptor encryptor;

    public Node(SelectionKey key) {
        super(key);
        postLock = new ReentrantLock();
        waitSendPacketQueue = new LinkedList<>();
    }

    /**
     * 检查是否在线
     *
     * @return 已连接且身份有效则返回true
     */
    public boolean isOnline() {
        return isConnect() && nodeInfo != null && nodeInfo.getUuid() != null;
    }

    /**
     * 添加包到待发送队列
     *
     * @param packetBody 待发送包
     */
    public void postPacket(PacketBody packetBody) {
        synchronized (waitSendPacketQueue) {
            waitSendPacketQueue.offer(packetBody);
            if (!postLock.isLocked()) {
                key.interestOps(key.interestOps() & SelectionKey.OP_WRITE);
                key.selector().wakeup();
            }
        }
    }

    /**
     * 发送包
     */
    public void doPost() {
        synchronized (waitSendPacketQueue) {
            if (postLock.isLocked()) {
                return;
            } else {
                postLock.lock();
            }
        }
        PacketBody packetBody;
        while (true) {
            synchronized (waitSendPacketQueue) {
                if (waitSendPacketQueue.size() > 0) {
                    packetBody = waitSendPacketQueue.poll();
                } else {
                    postLock.unlock();
                    return;
                }
            }
            // TODO: 2022/4/27
        }
    }

    /**
     * 数据包处理
     *
     * @param data 数据
     */
    @Override
    protected void dataHandler(byte[] data) throws Exception {
        // TODO: 2022/4/26
    }

    @Override
    public void disconnect() throws Exception {
        super.disconnect();
        // TODO: 2022/4/26
    }

    /**
     * 异常处理
     *
     * @param e 异常
     */
    @Override
    protected void exceptionHandler(Exception e) {
        Logger.error(e);
        try {
            disconnect();
        } catch (Exception ex) {
            Logger.warn(ex);
        }
    }

    /**
     * 初始化完成
     */
    abstract void initDone();

    /**
     * 初始化失败
     */
    abstract void initFail();
}
