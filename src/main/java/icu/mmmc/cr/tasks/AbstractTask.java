package icu.mmmc.cr.tasks;

import icu.mmmc.cr.Node;
import icu.mmmc.cr.PacketBody;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.callbacks.adapters.ProgressAdapter;
import icu.mmmc.cr.constants.TaskTypes;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 抽象任务类
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public abstract class AbstractTask implements Task {
    /**
     * 时间戳
     */
    protected final long startTime;
    /**
     * 更新时间戳
     */
    protected long updateTime;
    /**
     * 所属节点
     */
    protected Node node;
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
        this.startTime = System.currentTimeMillis();
    }

    /**
     * 初始化任务
     */
    @Override
    public void init(Node node, int taskId) {
        this.node = node;
        this.taskId = taskId;
        callback.start();
    }

    /**
     * 处理包
     *
     * @param packetBody 包
     */
    @Override
    public void handlePacket(PacketBody packetBody) {
        if (packetBody.getTaskType() == TaskTypes.ERROR) {
            halt(new String(packetBody.getPayload(), StandardCharsets.UTF_8));
            return;
        }
        updateTime = System.currentTimeMillis();
    }

    /**
     * 终止
     *
     * @param msg 错误信息
     */
    @Override
    public void halt(String msg) {
        if (node != null) {
            node.removeTask(taskId);
        }
        callback.halt(msg);
    }

    /**
     * 结束任务
     */
    @Override
    public void done() {
        if (node != null) {
            node.removeTask(taskId);
        }
        callback.done();
    }

    /**
     * 获取开始时间戳
     *
     * @return 开始时间戳
     */
    @Override
    public long getStartTime() {
        return startTime;
    }

    /**
     * 获取最后更新时间
     *
     * @return 最后更新时间戳
     */
    @Override
    public long getUpdateTime() {
        return updateTime;
    }
}
