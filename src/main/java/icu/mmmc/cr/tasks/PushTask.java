package icu.mmmc.cr.tasks;

import icu.mmmc.cr.Node;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.entities.*;
import icu.mmmc.cr.exceptions.UnknownEntityTypeException;
import icu.mmmc.cr.utils.BsonObject;

import java.util.Objects;

/**
 * 推送任务
 * 推送实体信息
 *
 * @author shouchen
 */
public class PushTask extends TransmitTask {
    private final byte[] entityData;

    public PushTask(Serialization entity, ProgressCallback callback) throws Exception {
        super(callback);
        if (Objects.equals(entity.getClass(), NodeInfo.class)) {
            ((NodeInfo) entity).check();
            entityType = ENTITY_NODE_INFO;
        } else if (Objects.equals(entity.getClass(), RoomInfo.class)) {
            ((RoomInfo) entity).check();
            entityType = ENTITY_ROOM_INFO;
        } else if (Objects.equals(entity.getClass(), MemberInfo.class)) {
            ((MemberInfo) entity).check();
            entityType = ENTITY_MEMBER_INFO;
        } else if (Objects.equals(entity.getClass(), MessageInfo.class)) {
            ((MessageInfo) entity).check();
            entityType = ENTITY_MESSAGE_INFO;
        } else {
            throw new UnknownEntityTypeException();
        }
        entityData = entity.serialize();
        if (entityData.length > MAX_DATA_LENGTH) {
            throw new Exception("Data length out of range");
        }
    }

    /**
     * 处理数据
     *
     * @param data 数据
     */
    @Override
    protected void handleData(byte[] data) {
        sendData(TaskTypes.ACK, entityData);
        done();
    }

    @Override
    public void init(Node node, int taskId) {
        super.init(node, taskId);
        sendData(TaskTypes.PUSH, new BsonObject()
                .set(ENTITY_TYPE, entityType)
                .set(DATA_LENGTH, entityData.length)
                .serialize());
    }
}
