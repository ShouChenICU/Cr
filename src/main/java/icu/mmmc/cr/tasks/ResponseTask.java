package icu.mmmc.cr.tasks;

import icu.mmmc.cr.ChatPavilion;
import icu.mmmc.cr.ChatRoomManager;
import icu.mmmc.cr.Cr;
import icu.mmmc.cr.callbacks.ChatRoomUpdateCallback;
import icu.mmmc.cr.callbacks.MsgReceiveCallback;
import icu.mmmc.cr.constants.MemberRoles;
import icu.mmmc.cr.constants.RequestTypes;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.entities.MemberInfo;
import icu.mmmc.cr.entities.MessageInfo;
import icu.mmmc.cr.exceptions.AuthenticationException;
import icu.mmmc.cr.utils.BsonUtils;
import icu.mmmc.cr.utils.Logger;
import org.bson.BSONObject;

import java.util.Objects;

/**
 * 响应请求任务
 *
 * @author shouchen
 */
public class ResponseTask extends AbstractTask {

    public ResponseTask() {
        super(null);
    }

    /**
     * 处理数据
     *
     * @param data 数据
     */
    @Override
    protected void handleData(byte[] data) throws Exception {
        try {
            BSONObject object = BsonUtils.deserialize(data);
            int requestType = (int) object.get("REQ_TYPE");
            switch (requestType) {
                case RequestTypes.ADD_MEMBER:
                    String nodeUUID = (String) object.get("0");
                    String roomUUID = (String) object.get("1");
                    String userUUID = (String) object.get("2");

                    ChatPavilion pavilion = (ChatPavilion) ChatRoomManager.getByUUID(nodeUUID, roomUUID);
                    if (pavilion == null) {
                        throw new Exception("Room not found");
                    }
                    MemberInfo info = pavilion.getMemberInfo(node.getNodeInfo().getUUID());
                    if (!Objects.equals(pavilion.getRoomInfo().getNodeUUID(), node.getNodeInfo().getUUID())
                            && info.getRole() != MemberRoles.ROLE_ADMIN) {
                        throw new AuthenticationException("Permission deny");
                    }
                    pavilion.addMember(userUUID);
                    break;
                case RequestTypes.DEL_MEMBER:
                    nodeUUID = (String) object.get("0");
                    roomUUID = (String) object.get("1");
                    userUUID = (String) object.get("2");
                    pavilion = (ChatPavilion) ChatRoomManager.getByUUID(nodeUUID, roomUUID);
                    if (pavilion == null) {
                        throw new Exception("Room not found");
                    }
                    info = pavilion.getMemberInfo(node.getNodeInfo().getUUID());
                    if (Objects.equals(pavilion.getRoomInfo().getNodeUUID(), node.getNodeInfo().getUUID())) {
                        pavilion.deleteMember(userUUID);
                    } else if (info.getRole() == MemberRoles.ROLE_ADMIN) {
                        pavilion.removeMember(userUUID);
                    } else {
                        throw new AuthenticationException("Permission deny");
                    }
                    break;
                case RequestTypes.UPDATE_ROOM_TITLE:
                    String title = (String) object.get("0");
                    nodeUUID = (String) object.get("1");
                    roomUUID = (String) object.get("2");
                    pavilion = (ChatPavilion) ChatRoomManager.getByUUID(nodeUUID, roomUUID);
                    if (pavilion == null) {
                        throw new Exception("Room not found");
                    }
                    pavilion.updateRoomTitle(title);
                    ChatRoomUpdateCallback callback = Cr.CallBack.chatRoomUpdateCallback;
                    if (callback != null) {
                        callback.update();
                    }
                    break;
                case RequestTypes.UPDATE_NICKNAME:
                    String nickname = (String) object.get("0");
                    nodeUUID = (String) object.get("1");
                    roomUUID = (String) object.get("2");
                    userUUID = node.getNodeInfo().getUUID();
                    pavilion = (ChatPavilion) ChatRoomManager.getByUUID(nodeUUID, roomUUID);
                    if (pavilion == null) {
                        throw new Exception("Room not found");
                    }
                    MemberInfo memberInfo = pavilion.getMemberInfo(userUUID);
                    if (memberInfo == null) {
                        throw new Exception("Member not found");
                    }
                    memberInfo.setNickname(nickname);
                    pavilion.updateMemberInfo(memberInfo);
                    break;
                case RequestTypes.UPDATE_LABEL:
                    String label = (String) object.get("0");
                    nodeUUID = (String) object.get("1");
                    roomUUID = (String) object.get("2");
                    userUUID = node.getNodeInfo().getUUID();
                    pavilion = (ChatPavilion) ChatRoomManager.getByUUID(nodeUUID, roomUUID);
                    if (pavilion == null) {
                        throw new Exception("Room not found");
                    }
                    memberInfo = pavilion.getMemberInfo(userUUID);
                    if (memberInfo == null) {
                        throw new Exception("Member not found");
                    }
                    memberInfo.setLabel(label);
                    pavilion.updateMemberInfo(memberInfo);
                    break;
                case RequestTypes.SEND_TEXT_MSG:
                    nodeUUID = (String) object.get("0");
                    roomUUID = (String) object.get("1");
                    MessageInfo messageInfo = new MessageInfo((byte[]) object.get("2"));
                    pavilion = (ChatPavilion) ChatRoomManager.getByUUID(nodeUUID, roomUUID);
                    if (pavilion == null) {
                        throw new Exception("Room not found");
                    }
                    pavilion.receiveMessage(messageInfo);
                    MsgReceiveCallback callback1 = Cr.CallBack.msgReceiveCallback;
                    if (callback1 != null) {
                        callback1.receiveMsg(pavilion, messageInfo);
                    }
                    break;
            }
        } catch (Exception e) {
            Logger.warn(e);
            sendError(e.getMessage());
            halt(e.getMessage());
            return;
        }
        sendData(TaskTypes.ACK, null);
        done();
    }
}
