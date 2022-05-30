package icu.mmmc.cr.tasks;

import icu.mmmc.cr.Node;
import icu.mmmc.cr.PacketBody;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.callbacks.adapters.ProgressAdapter;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.utils.BsonObject;
import icu.mmmc.cr.utils.BsonUtils;
import org.bson.BSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 抽象任务类
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public abstract class AbstractTask implements Task {
    private static final int MAX_DATA_LENGTH = 64000;
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
    /**
     * 目标任务id
     */
    protected int destinationId;
    /**
     * 数据缓冲
     */
    private byte[] buffer;
    /**
     * 已处理长度
     */
    private int handledLength;
    /**
     * 收发模式
     * 为true是发送模式，false是接收模式
     */
    private boolean isSendMode;

    public AbstractTask(ProgressCallback callback) {
        this.callback = Objects.requireNonNullElse(callback, new ProgressAdapter());
        this.destinationId = 0;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * 初始化任务
     *
     * @param node   节点
     * @param taskId 分配的任务id
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
    public void handlePacket(PacketBody packetBody) throws Exception {
        destinationId = packetBody.getSource();
        if (packetBody.getTaskType() == TaskTypes.ERROR) {
            // 收到错误信息
            halt(new String(packetBody.getPayload(), StandardCharsets.UTF_8));
            return;
        } else if (buffer != null && handledLength < buffer.length) {
            // 处理分包
            if (isSendMode) {
                // 分包并发送
                int len = Math.min(buffer.length - handledLength, MAX_DATA_LENGTH);
                byte[] buf = new byte[len];
                System.arraycopy(buffer, handledLength, buf, 0, len);
                handledLength += len;
                node.postPacket(new PacketBody()
                        .setSource(taskId)
                        .setDestination(destinationId)
                        .setPayload(buf));
            } else {
                // 收到分包并合并
                byte[] buf = packetBody.getPayload();
                System.arraycopy(buf, 0, buffer, handledLength, buf.length);
                handledLength += buf.length;
                if (handledLength < buffer.length) {
                    // 分包未结束，继续请求数据
                    node.postPacket(new PacketBody()
                            .setSource(taskId)
                            .setDestination(destinationId)
                            .setTaskType(TaskTypes.ACK));
                } else {
                    // 转换为局部变量，方便GC处理
                    byte[] dat = buffer;
                    buffer = null;
                    // 分包结束，还原包，递归处理
                    handlePacket(new PacketBody(dat));
                }
            }
        } else if (packetBody.isMulti()) {
            // 分包模式
            BSONObject object = BsonUtils.deserialize(packetBody.getPayload());
            int length = (int) object.get("LENGTH");
            buffer = new byte[length];
            handledLength = 0;
            isSendMode = false;
            node.postPacket(new PacketBody()
                    .setSource(taskId)
                    .setDestination(destinationId)
                    .setTaskType(TaskTypes.ACK));
        } else {
            // 无分包，直接处理数据
            handleData(packetBody.getPayload());
        }
        // 更新时间戳
        updateTime = System.currentTimeMillis();
    }

    /**
     * 发送数据
     *
     * @param taskType 任务类型
     * @param data     数据
     */
    protected void sendData(int taskType, byte[] data) {
        if (data != null && data.length > MAX_DATA_LENGTH) {
            // 数据过大，开始分包
            buffer = new PacketBody().setSource(taskId)
                    .setDestination(destinationId)
                    .setTaskType(taskType)
                    .setPayload(data)
                    .serialize();
            handledLength = 0;
            isSendMode = true;
            node.postPacket(new PacketBody()
                    .setSource(taskId)
                    .setDestination(destinationId)
                    .setTaskType(taskType)
                    .setMulti(true)
                    .setPayload(new BsonObject()
                            .set("LENGTH", buffer.length)
                            .serialize()
                    ));
        } else {
            // 不需要分包，直接发送
            node.postPacket(new PacketBody()
                    .setSource(taskId)
                    .setDestination(destinationId)
                    .setTaskType(taskType)
                    .setPayload(data));
        }
    }

    /**
     * 发送错误信息
     *
     * @param err 错误信息
     */
    protected void sendError(String err) {
        sendData(TaskTypes.ERROR, err.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 处理数据
     *
     * @param data 数据
     */
    protected abstract void handleData(byte[] data) throws Exception;

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
