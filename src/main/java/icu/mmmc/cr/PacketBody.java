package icu.mmmc.cr;

import icu.mmmc.cr.entities.Serialization;
import icu.mmmc.cr.utils.BsonObject;
import icu.mmmc.cr.utils.BsonUtils;
import org.bson.BSONObject;

/**
 * 网络传输帧
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class PacketBody implements Serialization {
    /**
     * 任务来源id
     */
    private int source;
    /**
     * 目标任务id
     */
    private int destination;
    /**
     * 任务类型
     */
    private int taskType;
    /**
     * 分包标记
     */
    private boolean isMulti;
    /**
     * 负载数据
     */
    private byte[] payload;

    public PacketBody() {
        source = 0;
        destination = 0;
        taskType = 0;
        isMulti = false;
        payload = null;
    }

    public PacketBody(byte[] dat) {
        BSONObject object = BsonUtils.deserialize(dat);
        source = (int) object.get("SOURCE");
        destination = (int) object.get("DESTINATION");
        taskType = (int) object.get("TASK_TYPE");
        isMulti = (boolean) object.get("IS_MULTI");
        payload = (byte[]) object.get("PAYLOAD");
    }

    public int getSource() {
        return source;
    }

    public PacketBody setSource(int source) {
        this.source = source;
        return this;
    }

    public int getDestination() {
        return destination;
    }

    public PacketBody setDestination(int destination) {
        this.destination = destination;
        return this;
    }

    public int getTaskType() {
        return taskType;
    }

    public PacketBody setTaskType(int taskType) {
        this.taskType = taskType;
        return this;
    }

    public PacketBody setMulti(boolean multi) {
        isMulti = multi;
        return this;
    }

    public boolean isMulti() {
        return isMulti;
    }

    public byte[] getPayload() {
        return payload;
    }

    public PacketBody setPayload(byte[] payload) {
        this.payload = payload;
        return this;
    }

    /**
     * 序列化为Bson数据
     *
     * @return 序列化数据
     */
    @Override
    public byte[] serialize() {
        return new BsonObject()
                .set("SOURCE", source)
                .set("DESTINATION", destination)
                .set("TASK_TYPE", taskType)
                .set("IS_MULTI", isMulti)
                .set("PAYLOAD", payload)
                .serialize();
    }
}
