package icu.mmmc.cr.tasks;

import icu.mmmc.cr.Node;
import icu.mmmc.cr.PacketBody;
import icu.mmmc.cr.entities.Serialization;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.entities.MemberInfo;
import icu.mmmc.cr.entities.MessageInfo;
import icu.mmmc.cr.entities.NodeInfo;
import icu.mmmc.cr.entities.RoomInfo;
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
        data = entity.serialize();
    }

    @Override
    public void handlePacket(PacketBody packetBody) throws Exception {
        super.handlePacket(packetBody);
        if (processedLength == data.length) {
            done();
        } else {
            int len = Math.min((data.length - processedLength), 64000);
            byte[] buf = new byte[len];
            System.arraycopy(data, 0, buf, 0, len);
            processedLength += len;
            node.postPacket(
                    new PacketBody()
                            .setSource(taskId)
                            .setDestination(packetBody.getSource())
                            .setPayload(buf)
            );
        }
    }

    @Override
    public void init(Node node, int taskId) {
        super.init(node, taskId);
        node.postPacket(
                new PacketBody()
                        .setSource(taskId)
                        .setDestination(0)
                        .setTaskType(TaskTypes.PUSH)
                        .setPayload(
                                new BsonObject()
                                        .set(ENTITY_TYPE, entityType)
                                        .set(DATA_LENGTH, data.length)
                                        .serialize()
                        )
        );
    }
}
