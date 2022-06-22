package icu.mmmc.cr.tasks;

import icu.mmmc.cr.ChatPavilion;
import icu.mmmc.cr.ChatRoomManager;
import icu.mmmc.cr.Cr;
import icu.mmmc.cr.annotations.RequestPath;
import icu.mmmc.cr.callbacks.ChatRoomUpdateCallback;
import icu.mmmc.cr.callbacks.MsgReceiveCallback;
import icu.mmmc.cr.constants.MemberRoles;
import icu.mmmc.cr.constants.RequestPaths;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.entities.MemberInfo;
import icu.mmmc.cr.entities.MessageInfo;
import icu.mmmc.cr.entities.Serialization;
import icu.mmmc.cr.exceptions.AuthenticationException;
import icu.mmmc.cr.utils.BsonObject;
import icu.mmmc.cr.utils.BsonUtils;
import icu.mmmc.cr.utils.Logger;
import org.bson.BSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 响应请求任务
 *
 * @author shouchen
 */
@SuppressWarnings({"DuplicatedCode", "unused"})
public class ResponseTask extends AbstractRequestTask {
    private Object result;

    public ResponseTask() {
        super(null);
        result = null;
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
            // 获取请求路径
            String requestPath = ((String) object.get(REQ_PATH)).toUpperCase();
            // 获取具体方法
            Method method = PATH_METHOD_MAP.get(requestPath);
            if (method == null) {
                throw new Exception("Method not found");
            }
            // 反射拿到方法参数类型列表
            Class<?>[] parameterTypes = method.getParameterTypes();
            Object[] parameters = new Object[parameterTypes.length];
            // 填充参数
            for (int i = 0; i < parameterTypes.length; i++) {
                Object param = object.get(String.valueOf(i));
                if (param == null) {
                    throw new Exception("Illegal argument");
                }
                if (Serialization.class.isAssignableFrom(parameterTypes[i])) {
                    Constructor<?> constructor = parameterTypes[i].getDeclaredConstructor(param.getClass());
                    constructor.setAccessible(true);
                    parameters[i] = constructor.newInstance(param);
                } else {
                    parameters[i] = param;
                }
            }
            // 执行方法
            method.invoke(this, parameters);
        } catch (Exception e) {
            Logger.warn(e);
            sendError(Objects.requireNonNullElse(e.getMessage(), e.toString()));
            halt(e.getMessage());
            return;
        }
        sendData(TaskTypes.ACK, new BsonObject().set(RESULT, result).serialize());
        done(null);
    }

    @RequestPath(path = RequestPaths.ADD_MEMBER)
    private void addMember(String roomUUID, String userUUID) throws Exception {
        ChatPavilion pavilion = (ChatPavilion) ChatRoomManager.getManageRoomByUUID(roomUUID);
        if (pavilion == null) {
            throw new Exception("Room not found");
        }
        MemberInfo info = pavilion.getMemberInfo(node.getNodeInfo().getUUID());
        if (!Objects.equals(pavilion.getRoomInfo().getNodeUUID(), node.getNodeInfo().getUUID())
                && info.getRole() != MemberRoles.ROLE_ADMIN) {
            throw new AuthenticationException("Permission deny");
        }
        pavilion.addMember(userUUID);
    }

    @RequestPath(path = RequestPaths.DEL_MEMBER)
    private void delMember(String nodeUUID, String roomUUID, String userUUID) throws Exception {
        ChatPavilion pavilion = (ChatPavilion) ChatRoomManager.getByUUID(nodeUUID, roomUUID);
        if (pavilion == null) {
            throw new Exception("Room not found");
        }
        MemberInfo info = pavilion.getMemberInfo(node.getNodeInfo().getUUID());
        if (Objects.equals(pavilion.getRoomInfo().getNodeUUID(), node.getNodeInfo().getUUID())) {
            pavilion.deleteMember(userUUID);
        } else if (info.getRole() == MemberRoles.ROLE_ADMIN) {
            pavilion.removeMember(userUUID);
        } else {
            throw new AuthenticationException("Permission deny");
        }
    }

    @RequestPath(path = RequestPaths.UPDATE_ROOM_TITLE)
    private void updateRoomTitle(String title, String roomUUID) throws Exception {
        ChatPavilion pavilion = (ChatPavilion) ChatRoomManager.getManageRoomByUUID(roomUUID);
        if (pavilion == null) {
            throw new Exception("Room not found");
        }
        pavilion.updateRoomTitle(title);
        ChatRoomUpdateCallback callback = Cr.CallBack.chatRoomUpdateCallback;
        if (callback != null) {
            callback.update();
        }
    }

    @RequestPath(path = RequestPaths.UPDATE_NICKNAME)
    private void updateNickname(String nickname, String roomUUID) throws Exception {
        ChatPavilion pavilion = (ChatPavilion) ChatRoomManager.getManageRoomByUUID(roomUUID);
        if (pavilion == null) {
            throw new Exception("Room not found");
        }
        String userUUID = node.getNodeInfo().getUUID();
        MemberInfo memberInfo = pavilion.getMemberInfo(userUUID);
        if (memberInfo == null) {
            throw new Exception("Member not found");
        }
        memberInfo.setNickname(nickname);
        pavilion.updateMemberInfo(memberInfo);
    }

    @RequestPath(path = RequestPaths.UPDATE_LABEL)
    private void updateLabel(String label, String roomUUID) throws Exception {
        ChatPavilion pavilion = (ChatPavilion) ChatRoomManager.getManageRoomByUUID(roomUUID);
        if (pavilion == null) {
            throw new Exception("Room not found");
        }
        String userUUID = node.getNodeInfo().getUUID();
        MemberInfo memberInfo = pavilion.getMemberInfo(userUUID);
        if (memberInfo == null) {
            throw new Exception("Member not found");
        }
        memberInfo.setLabel(label);
        pavilion.updateMemberInfo(memberInfo);
    }

    @RequestPath(path = RequestPaths.SEND_TEXT_MSG)
    private void sendTextMessage(String roomUUID, MessageInfo messageInfo) throws Exception {
        ChatPavilion pavilion = (ChatPavilion) ChatRoomManager.getManageRoomByUUID(roomUUID);
        if (pavilion == null) {
            throw new Exception("Room not found");
        }
        pavilion.receiveMessage(messageInfo);
        MsgReceiveCallback callback = Cr.CallBack.msgReceiveCallback;
        if (callback != null) {
            callback.receiveMsg(pavilion, messageInfo);
        }
    }
}
