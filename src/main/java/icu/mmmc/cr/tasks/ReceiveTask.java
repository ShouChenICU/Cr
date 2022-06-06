package icu.mmmc.cr.tasks;

import icu.mmmc.cr.ChatPavilion;
import icu.mmmc.cr.ChatRoomManager;
import icu.mmmc.cr.Cr;
import icu.mmmc.cr.NodeManager;
import icu.mmmc.cr.callbacks.MsgReceiveCallback;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.entities.MemberInfo;
import icu.mmmc.cr.entities.MessageInfo;
import icu.mmmc.cr.entities.NodeInfo;
import icu.mmmc.cr.entities.RoomInfo;
import icu.mmmc.cr.utils.BsonUtils;
import icu.mmmc.cr.utils.Logger;
import org.bson.BSONObject;

import java.util.Objects;

/**
 * 接收任务
 *
 * @author shouchen
 */
public class ReceiveTask extends TransmitTask {
    private byte[] entityData;
    private int stepCount;

    public ReceiveTask() {
        super(null);
        stepCount = 0;
    }

    /**
     * 处理数据
     *
     * @param data 数据
     */
    @Override
    protected void handleData(byte[] data) {
        if (stepCount == 0) {
            BSONObject object = BsonUtils.deserialize(data);
            entityType = (int) object.get(ENTITY_TYPE);
            int length = (int) object.get(DATA_LENGTH);
            if (entityType != ENTITY_NODE_INFO
                    && entityType != ENTITY_ROOM_INFO
                    && entityType != ENTITY_MEMBER_INFO
                    && entityType != ENTITY_MESSAGE_INFO) {
                String s = "unknown entity type";
                sendError(s);
                halt(s);
                return;
            } else if (length > MAX_DATA_LENGTH) {
                String s = "Data length out of range";
                sendError(s);
                halt(s);
                return;
            }
            sendData(TaskTypes.ACK, null);
            stepCount = 1;
        } else {
            entityData = data;
            handle();
        }
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
        NodeInfo nodeInfo = new NodeInfo(entityData);
        NodeManager.updateNodeInfo(nodeInfo);
    }

    /**
     * 房间信息接收处理
     */
    private void receiveRoomInfo() throws Exception {
        RoomInfo roomInfo = new RoomInfo(entityData);
        if (!Objects.equals(roomInfo.getNodeUUID(), node.getNodeInfo().getUUID())) {
            throw new Exception("Room info illegal");
        }
        ChatRoomManager.updateRoomInfo(roomInfo);
    }

    /**
     * 成员信息接收处理
     */
    private void receiveMemberInfo() throws Exception {
        MemberInfo memberInfo = new MemberInfo(entityData);
        memberInfo.check();
        if (!Objects.equals(memberInfo.getNodeUUID(), node.getNodeInfo().getUUID())) {
            throw new Exception("Member illegal");
        }
        ChatPavilion chatPavilion = (ChatPavilion) ChatRoomManager.getByUUID(memberInfo.getNodeUUID(), memberInfo.getRoomUUID());
        if (chatPavilion == null) {
            throw new Exception("Chat room not found");
        }
        chatPavilion.updateMemberInfo(memberInfo);
    }

    /**
     * 消息接收处理
     */
    private void receiveMessageInfo() throws Exception {
        MessageInfo messageInfo = new MessageInfo(entityData);
        messageInfo.check();
        if (!Objects.equals(messageInfo.getNodeUUID(), node.getNodeInfo().getUUID())) {
            throw new Exception("Message illegal");
        }
        ChatPavilion chatPavilion = (ChatPavilion) ChatRoomManager.getByUUID(messageInfo.getNodeUUID(), messageInfo.getRoomUUID());
        if (chatPavilion == null) {
            throw new Exception("Chat room not found");
        }
        MsgReceiveCallback callback = Cr.CallBack.msgReceiveCallback;
        if (callback != null) {
            callback.receiveMsg(messageInfo);
        }
        chatPavilion.putMessage(messageInfo);
    }
}
