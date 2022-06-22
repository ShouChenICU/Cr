package icu.mmmc.cr;

import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.entities.NodeInfo;
import icu.mmmc.cr.tasks.*;
import icu.mmmc.cr.utils.Logger;

import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 节点
 * 一个节点就代表一个Cr实例
 *
 * @author shouchen
 */
public abstract class Node extends NetNode {
    private static final long TASK_TIMEOUT = TimeUnit.SECONDS.toMillis(180);
    private static final ScheduledThreadPoolExecutor TIMER = new ScheduledThreadPoolExecutor(1, r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });
    private final ReentrantLock postLock;
    private final Queue<PacketBody> waitSendPacketQueue;
    private final Encryptor encryptor;
    private final ConcurrentHashMap<Integer, Task> taskMap;
    protected NodeInfo nodeInfo;
    protected long heartBeat;
    private int taskIdCount;
    private int sendPacketCount;
    private int receivePacketCount;

    public Node(SelectionKey key) throws Exception {
        super(key);
        postLock = new ReentrantLock();
        waitSendPacketQueue = new LinkedList<>();
        encryptor = new Encryptor();
        taskMap = new ConcurrentHashMap<>();
        heartBeat = System.currentTimeMillis();
        taskIdCount = 1;
        sendPacketCount = 0;
        receivePacketCount = 0;
    }

    /**
     * 轮询检查任务是否超时
     */
    protected void checkTaskTimeOut() {
        if (isConnect()) {
            for (Task task : taskMap.values()) {
                if (task.getUpdateTime() - task.getStartTime() > TASK_TIMEOUT) {
                    task.halt("Task time out");
                }
            }
            TIMER.schedule(this::checkTaskTimeOut, 30, TimeUnit.SECONDS);
        }
    }

    /**
     * 设置节点信息
     *
     * @param nodeInfo 节点信息
     */
    public void setNodeInfo(NodeInfo nodeInfo) {
        synchronized (this) {
            this.nodeInfo = nodeInfo;
        }
    }

    /**
     * 获取节点信息
     *
     * @return 节点信息
     */
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
            // 防止溢出
            if (taskIdCount == Integer.MAX_VALUE) {
                taskIdCount = 1;
            }
        }
        Logger.debug("Add task " + task.getClass().getSimpleName() + " id:" + id);
        task.init(this, id);
    }

    /**
     * 移除任务
     *
     * @param taskId 任务id
     */
    public void removeTask(int taskId) {
        if (taskMap.remove(taskId) != null) {
            Logger.debug("Remove task " + taskId);
        }
    }

    /**
     * 检查是否在线
     *
     * @return 已连接且身份有效则返回true
     */
    protected boolean isOnline() {
        return isConnect() && nodeInfo != null && nodeInfo.getUUID() != null;
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
     * 阻塞式的发送包，等待发送成功后返回
     *
     * @param packetBody 数据包
     */
    public boolean postPacketBlocked(PacketBody packetBody) {
        if (packetBody == null) {
            return false;
        }
        try {
            if (!postLock.tryLock(5, TimeUnit.SECONDS)) {
                return false;
            }
        } catch (InterruptedException e) {
            Logger.warn(e);
            return false;
        }
        try {
            byte[] dat = encryptor.encrypt(packetBody.serialize());
            doWrite(dat);
            return true;
        } catch (Exception e) {
            Logger.warn(e);
            try {
                disconnect(Objects.requireNonNullElse(e.getMessage(), e.toString()));
            } catch (Exception ex) {
                Logger.warn(ex);
            }
            return false;
        } finally {
            postLock.unlock();
            synchronized (waitSendPacketQueue) {
                if (!postLock.isLocked() && isConnect() && !waitSendPacketQueue.isEmpty()) {
                    key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                    key.selector().wakeup();
                }
            }
        }
    }

    /**
     * 发送包
     */
    protected void doPost() {
        synchronized (waitSendPacketQueue) {
            if (!postLock.tryLock()) {
                return;
            }
        }
        PacketBody packetBody;
        while (true) {
            synchronized (waitSendPacketQueue) {
                if (waitSendPacketQueue.size() > 0) {
                    packetBody = waitSendPacketQueue.poll();
                    sendPacketCount++;
                } else {
                    postLock.unlock();
                    return;
                }
            }
            try {
                byte[] dat = encryptor.encrypt(packetBody.serialize());
                doWrite(dat);
            } catch (Exception e) {
                postLock.unlock();
                Logger.warn(e);
                try {
                    disconnect(Objects.requireNonNullElse(e.getMessage(), e.toString()));
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
        heartBeat = System.currentTimeMillis();
        Logger.debug("Data handler, length = " + data.length);
        PacketBody packetBody = new PacketBody(encryptor.decrypt(data));
        receivePacketCount++;
        Task task;
        if (packetBody.getDestination() == 0) {
            switch (packetBody.getTaskType()) {
                case TaskTypes.DISCONNECT:
                    disconnect(new String(packetBody.getPayload(), StandardCharsets.UTF_8));
                    return;
                case TaskTypes.PING:
                    postPacketBlocked(packetBody.setTaskType(TaskTypes.PONG));
                    return;
                case TaskTypes.PONG:
                    return;
                case TaskTypes.INIT:
                    task = new InitTask0();
                    break;
                case TaskTypes.PUSH:
                    task = new ReceiveTask();
                    break;
                case TaskTypes.REQUEST:
                    task = new ResponseTask();
                    break;
                case TaskTypes.SYNC_ROOM:
                    task = new SyncRoomTask0();
                    break;
                case TaskTypes.SYNC_MEMBER:
                    task = new SyncMemberTask0();
                    break;
                case TaskTypes.SYNC_MESSAGE:
                    task = new SyncMessageTask0();
                    break;
                default:
                    throw new Exception("Unknown task");
            }
            addTask(task);
        } else {
            task = taskMap.get(packetBody.getDestination());
        }
        if (task == null) {
            throw new Exception("Task not found");
        }
        task.handlePacket(packetBody);
    }

    @Override
    public void disconnect(String reason) throws Exception {
        super.disconnect(reason);
        synchronized (taskMap) {
            for (Task task : taskMap.values()) {
                task.halt("Disconnect: " + reason);
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
        Logger.debug(e);
        try {
            disconnect(Objects.requireNonNullElse(e.getMessage(), e.toString()));
        } catch (Exception ex) {
            Logger.warn(ex);
        }
    }

    public long getHeartBeat() {
        return heartBeat;
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
