package icu.mmmc.cr.tasks;

import icu.mmmc.cr.*;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.entities.MemberInfo;
import icu.mmmc.cr.entities.MessageInfo;
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
            int length = (int) object.get(DATA_LENGTH);
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
            } else if (length > MAX_DATA_LENGTH) {
                String s = "Data length out of range";
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
            data = new byte[length];
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

    /**
     * 数据处理
     */
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

    /**
     * 节点信息接收处理
     */
    private void receiveNodeInfo() throws Exception {
        NodeInfo nodeInfo = new NodeInfo(data);
        NodeManager.updateNodeInfo(nodeInfo);
    }

    /**
     * 房间信息接收处理
     */
    private void receiveRoomInfo() throws Exception {
        RoomInfo roomInfo = new RoomInfo(data);
        if (!Objects.equals(roomInfo.getNodeUUID(), node.getNodeInfo().getUuid())) {
            throw new Exception("Room info illegal");
        }
        ChatRoomManager.updateRoomInfo(roomInfo);
    }

    /**
     * 成员信息接收处理
     */
    private void receiveMemberInfo() throws Exception {
        MemberInfo memberInfo = new MemberInfo(data);
        memberInfo.check();
        // 如果这个成员所属的房间归我管理
        if (Objects.equals(memberInfo.getNodeUUID(), Cr.getNodeInfo().getUuid())) {
            // 那么对方只能修改自己的属性，比如昵称什么的，否则就是在搞事情
            if (!Objects.equals(memberInfo.getUserUUID(), node.getNodeInfo().getUuid())) {
                throw new Exception("Member illegal");
            }
        } else if (!Objects.equals(memberInfo.getNodeUUID(), node.getNodeInfo().getUuid())) {
            // 这个成员所属的房间既不归我也不归他，那么这个成员信息就是无中生有
            throw new Exception("Member fabricated");
        }
        ChatPavilion chatPavilion = (ChatPavilion) node.getRoomMap().get(memberInfo.getRoomUUID());
        if (chatPavilion == null) {
            throw new Exception("Chat room not found");
        }
        chatPavilion.updateMemberInfo(memberInfo);
    }

    /**
     * 消息接收处理
     */
    private void receiveMessageInfo() throws Exception {
        MessageInfo messageInfo = new MessageInfo(data);
        messageInfo.check();
        // 如果这个消息所属的房间归我管理
        if (Objects.equals(messageInfo.getNodeUUID(), Cr.getNodeInfo().getUuid())) {
            // 那么消息的发送者必须是对方
            if (!Objects.equals(messageInfo.getSenderUUID(), node.getNodeInfo().getUuid())) {
                throw new Exception("Message illegal");
            }
        } else if (!Objects.equals(messageInfo.getNodeUUID(), node.getNodeInfo().getUuid())) {
            // 这个消息所属的房间既不归我也不归他，那么这个消息就是无中生有
            throw new Exception("Message fabricated");
        }
        ChatPavilion chatPavilion = (ChatPavilion) node.getRoomMap().get(messageInfo.getRoomUUID());
        if (chatPavilion == null) {
            throw new Exception("Chat room not found");
        }
        chatPavilion.putMessage(messageInfo);
    }
}
