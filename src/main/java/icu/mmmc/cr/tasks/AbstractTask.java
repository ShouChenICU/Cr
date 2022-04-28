package icu.mmmc.cr.tasks;

import icu.mmmc.cr.Node;
import icu.mmmc.cr.PacketBody;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.callbacks.adapters.ProgressAdapter;

import java.util.Objects;

/**
 * 抽象任务类
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public abstract class AbstractTask {
    /**
     * 时间戳
     */
    protected final long timeStamp;
    /**
     * 所属节点
     */
    private Node node;
    /**
     * 任务id
     */
    protected int taskId;
    /**
     * 进度回调
     */
    protected ProgressCallback callback;

    public AbstractTask(ProgressCallback callback) {
        this.callback = Objects.requireNonNullElse(callback, new ProgressAdapter());
        this.timeStamp = System.currentTimeMillis();
    }

    /**
     * 初始化任务
     */
    public void init(Node node, int taskId) {
        this.node = node;
        this.taskId = taskId;
        callback.start();
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    /**
     * 处理包
     *
     * @param packetBody 包
     */
    public abstract void handlePacket(PacketBody packetBody);

    /**
     * 终止
     *
     * @param msg 错误信息
     */
    public void halt(String msg) {
        node.removeTask(taskId);
        callback.halt(msg);
    }

    /**
     * 结束任务
     */
    public void done() {
        node.removeTask(taskId);
        callback.done();
    }
}
