package icu.mmmc.cr;

import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.entities.NodeInfo;
import icu.mmmc.cr.tasks.InitTask0;
import icu.mmmc.cr.tasks.ReceiveTask;
import icu.mmmc.cr.tasks.Task;
import icu.mmmc.cr.utils.Logger;

import java.nio.channels.SelectionKey;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 节点
 * 一个节点就代表一个Cr实例
 *
 * @author shouchen
 */
public abstract class Node extends NetNode {
    private final ReentrantLock postLock;
    private final Queue<PacketBody> waitSendPacketQueue;
    private final Encryptor encryptor;
    private final ConcurrentHashMap<Integer, Task> taskMap;
    protected NodeInfo nodeInfo;
    private int taskIdCount;
    private int sendPacketCount;
    private int receivePacketCount;

    public Node(SelectionKey key) throws Exception {
        super(key);
        postLock = new ReentrantLock();
        waitSendPacketQueue = new LinkedList<>();
        encryptor = new Encryptor();
        taskMap = new ConcurrentHashMap<>();
        taskIdCount = 1;
        sendPacketCount = 0;
        receivePacketCount = 0;
    }

    public void setNodeInfo(NodeInfo nodeInfo) {
        synchronized (this) {
            if (this.nodeInfo == null) {
                this.nodeInfo = nodeInfo;
            }
        }
    }

    public NodeInfo getNodeInfo() {
        return nodeInfo;
    }

    /**
     * 添加任务
     *
     * @param task 任务
     */
    public void addTask(Task task) {
        int id;
        synchronized (taskMap) {
            id = taskIdCount++;
            taskMap.put(id, task);
        }
        Logger.debug("add task " + task.getClass().getSimpleName() + " id:" + id);
        task.init(this, id);
    }

    /**
     * 移除任务
     *
     * @param taskId 任务id
     */
    public void removeTask(int taskId) {
        if (taskMap.remove(taskId) != null) {
            Logger.debug("remove task " + taskId);
        }
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
            if (!postLock.isLocked() && isConnect()) {
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
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
                    sendPacketCount++;
                    return;
                }
            }
            try {
                byte[] dat = encryptor.encrypt(packetBody.serialize());
                doWrite(dat);
            } catch (Exception e) {
                Logger.warn(e);
                try {
                    disconnect();
                } catch (Exception ex) {
                    Logger.warn(ex);
                }
                return;
            }
        }
    }

    /**
     * 获取加解密器
     *
     * @return 加解密器
     */
    public Encryptor getEncryptor() {
        return encryptor;
    }

    /**
     * 数据包处理
     *
     * @param data 数据
     */
    @Override
    protected void dataHandler(byte[] data) throws Exception {
        receivePacketCount++;
        Logger.debug("data handler, length = " + data.length);
        PacketBody packetBody = new PacketBody(encryptor.decrypt(data));
        Task task;
        if (packetBody.getDestination() == 0) {
            switch (packetBody.getTaskType()) {
                case TaskTypes.INIT:
                    task = new InitTask0();
                    break;
                case TaskTypes.PUSH:
                    task = new ReceiveTask();
                    break;
                default:
                    throw new Exception("unknown task");
            }
            addTask(task);
        } else {
            task = taskMap.get(packetBody.getDestination());
        }
        if (task == null) {
            throw new Exception("task not found");
        }
        Task finalTask = task;
        WorkerThreadPool.execute(() -> {
            synchronized (finalTask) {
                finalTask.handlePacket(packetBody);
            }
        });
    }

    @Override
    public void disconnect() throws Exception {
        super.disconnect();
        synchronized (taskMap) {
            for (Task task : taskMap.values()) {
                task.halt("disconnect");
            }
        }
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
    public abstract void initDone();

    /**
     * 初始化失败
     */
    public abstract void initFail();
}
