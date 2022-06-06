package icu.mmmc.cr.tasks;

import icu.mmmc.cr.ChatPavilion;
import icu.mmmc.cr.ChatRoomManager;
import icu.mmmc.cr.constants.MemberRoles;
import icu.mmmc.cr.constants.RequestTypes;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.entities.MemberInfo;
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
                    MemberInfo memberInfo = new MemberInfo((byte[]) object.get("0"));
                    memberInfo.check();
                    ChatPavilion pavilion = (ChatPavilion) ChatRoomManager.getByUUID(memberInfo.getNodeUUID(), memberInfo.getRoomUUID());
                    if (pavilion == null) {
                        throw new Exception("Room not found");
                    }
                    MemberInfo info = pavilion.getMemberInfo(node.getNodeInfo().getUUID());
                    if (!Objects.equals(pavilion.getRoomInfo().getNodeUUID(), node.getNodeInfo().getUUID())
                            && info.getRole() != MemberRoles.ROLE_ADMIN) {
                        throw new AuthenticationException("Permission deny");
                    }
                    pavilion.updateMemberInfo(memberInfo);
                    break;
                case RequestTypes.DEL_MEMBER:
                    String nodeUUID = (String) object.get("0");
                    String roomUUID = (String) object.get("1");
                    String userUUID = (String) object.get("2");
                    pavilion = (ChatPavilion) ChatRoomManager.getByUUID(nodeUUID, roomUUID);
                    if (pavilion == null) {
                        throw new Exception("Room not found");
                    }
                    info = pavilion.getMemberInfo(node.getNodeInfo().getUUID());
                    if (Objects.equals(pavilion.getRoomInfo().getNodeUUID(), node.getNodeInfo().getUUID())) {
                        pavilion.deleteMember(userUUID);
                    } else if (info.getRole() == MemberRoles.ROLE_ADMIN) {
                        pavilion.deleteMember(userUUID);
                    } else {
                        throw new AuthenticationException("Permission deny");
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
                    memberInfo = pavilion.getMemberInfo(userUUID);
                    if (memberInfo == null) {
                        throw new Exception("Member not found");
                    }
                    memberInfo.setNickname(nickname);
                    pavilion.updateMemberInfo(memberInfo);
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
