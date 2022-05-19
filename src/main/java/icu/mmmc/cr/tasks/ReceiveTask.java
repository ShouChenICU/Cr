package icu.mmmc.cr.tasks;

import icu.mmmc.cr.ChatRoomManager;
import icu.mmmc.cr.NodeManager;
import icu.mmmc.cr.PacketBody;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.entities.NodeInfo;
import icu.mmmc.cr.entities.RoomInfo;
import icu.mmmc.cr.utils.BsonUtils;
import icu.mmmc.cr.utils.Logger;
import org.bson.BSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 接收任务
 *
 * @author shouchen
 */
public class ReceiveTask extends TransmitTask {
    private int stepCount;

    public ReceiveTask() {
        super(null);
        stepCount = 0;
    }

    /**
     * 处理包
     *
     * @param packetBody 包
     */
    @Override
    public void handlePacket(PacketBody packetBody) throws Exception {
        super.handlePacket(packetBody);
        if (stepCount == 0) {
            BSONObject object = BsonUtils.deserialize(packetBody.getPayload());
            entityType = (int) object.get(ENTITY_TYPE);
            data = new byte[(int) object.get(DATA_LENGTH)];
            if (entityType != ENTITY_NODE_INFO
                    && entityType != ENTITY_ROOM_INFO
                    && entityType != ENTITY_MEMBER_INFO
                    && entityType != ENTITY_MESSAGE_INFO) {
                String s = "unknown entity type";
                node.postPacket(
                        new PacketBody()
                                .setSource(taskId)
                                .setDestination(packetBody.getSource())
                                .setTaskType(TaskTypes.ERROR)
                                .setPayload(s.getBytes(StandardCharsets.UTF_8))
                );
                halt(s);
                return;
            }
            stepCount = 1;
        } else {
            byte[] buf = packetBody.getPayload();
            System.arraycopy(buf, 0, data, processedLength, buf.length);
            processedLength += buf.length;
            if (processedLength == data.length) {
                handle();
            }
        }
        node.postPacket(
                new PacketBody()
                        .setSource(taskId)
                        .setDestination(packetBody.getSource())
                        .setTaskType(TaskTypes.ACK)
        );
    }

    public void handle() {
        try {
            switch (entityType) {
                case ENTITY_NODE_INFO:
                    receiveNodeInfo();
                    break;
                case ENTITY_ROOM_INFO:
                    receiveRoomInfo();
                    break;
                case ENTITY_MEMBER_INFO:
                    receiveMemberInfo();
                    break;
                case ENTITY_MESSAGE_INFO:
                    receiveMessageInfo();
                    break;
            }
            done();
        } catch (Exception e) {
            Logger.warn(e);
            halt(Objects.requireNonNullElse(e.getMessage(), e.toString()));
        }
    }

    private void receiveNodeInfo() throws Exception {
        NodeInfo nodeInfo = new NodeInfo(data);
        NodeManager.updateNodeInfo(nodeInfo);
    }

    private void receiveRoomInfo() throws Exception {
        RoomInfo roomInfo = new RoomInfo(data);
        ChatRoomManager.updateRoomInfo(roomInfo);
    }

    private void receiveMemberInfo() {

    }

    private void receiveMessageInfo() {

    }
}
