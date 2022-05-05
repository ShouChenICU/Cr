package icu.mmmc.cr;

import icu.mmmc.cr.callbacks.MsgReceiveCallback;
import icu.mmmc.cr.entities.MemberInfo;
import icu.mmmc.cr.entities.MessageInfo;
import icu.mmmc.cr.entities.RoomInfo;
import icu.mmmc.cr.utils.Logger;

import java.util.*;

/**
 * 聊天室实体
 * 包含房间详细信息，成员列表，消息列表和在线节点列表
 * 其中在线节点列表仅由房主节点维护，用以广播消息时快速索引在线节点
 *
 * @author shouchen
 */
public class ChatRoom {
    /**
     * 缓存的消息列表长度
     */
    private static final int MSG_LIST_BUF_SIZE = 20;
    /**
     * 房间信息
     */
    private RoomInfo roomInfo;
    /**
     * 成员map
     */
    private final Map<String, MemberInfo> memberMap;
    /**
     * 在线节点map
     */
    private final Map<String, Node> onlineNodeMap;
    /**
     * 消息列表
     */
    private final List<MessageInfo> messageList;

    private MsgReceiveCallback msgReceiveCallback;

    public ChatRoom(RoomInfo roomInfo) {
        this.roomInfo = roomInfo;
        this.memberMap = new HashMap<>();
        onlineNodeMap = new HashMap<>();
        messageList = new ArrayList<>();
    }

    /**
     * 获取房间信息
     *
     * @return 房间信息
     */
    public RoomInfo getRoomInfo() {
        return roomInfo;
    }

    /**
     * 更新房间信息
     *
     * @param roomInfo 房间信息
     */
    public void updateRoomInfo(RoomInfo roomInfo) {
        synchronized (this) {
            this.roomInfo = roomInfo;
        }
    }

    /**
     * 获取成员列表
     *
     * @return 成员列表
     */
    public List<MemberInfo> getMemberList() {
        synchronized (memberMap) {
            return new ArrayList<>(memberMap.values());
        }
    }

    /**
     * 更新成员信息
     *
     * @param memberInfo 成员信息
     */
    public void updateMemberInfo(MemberInfo memberInfo) {
        synchronized (memberMap) {
            memberMap.put(memberInfo.getUserUUID(), memberInfo);
        }
    }

    /**
     * 更新成员列表
     *
     * @param memberInfoList 成员信息列表
     */
    public void updateMemberList(List<MemberInfo> memberInfoList) {
        synchronized (memberMap) {
            memberMap.clear();
            for (MemberInfo info : memberInfoList) {
                memberMap.put(info.getUserUUID(), info);
            }
        }
    }

    /**
     * 是否存在成员
     *
     * @param uuid 成员标识码
     * @return 如果存在该成员则返回true, 否则返回false
     */
    public boolean containsMember(String uuid) {
        synchronized (memberMap) {
            return memberMap.containsKey(uuid);
        }
    }

    /**
     * 添加在线节点
     *
     * @param uuid 节点标识码
     * @param node 节点实体
     */
    public void putNode(String uuid, Node node) {
        synchronized (onlineNodeMap) {
            onlineNodeMap.put(uuid, node);
        }
    }

    /**
     * 移除节点
     *
     * @param uuid 节点标识码
     */
    public void removeNode(String uuid) {
        synchronized (onlineNodeMap) {
            onlineNodeMap.remove(uuid);
        }
    }

    /**
     * 添加消息
     *
     * @param messageInfo 消息实体
     */
    public void putMessage(MessageInfo messageInfo) {
        synchronized (messageList) {
            messageList.add(messageInfo);
            messageList.sort(Comparator.comparingLong(MessageInfo::getTimestamp));
            if (messageList.size() > MSG_LIST_BUF_SIZE) {
                messageList.remove(0);
            }
            if (msgReceiveCallback != null) {
                try {
                    msgReceiveCallback.receiveMsg(messageInfo);
                } catch (Exception e) {
                    Logger.warn(e);
                }
            }
        }
    }

    /**
     * 获取消息列表
     *
     * @return 消息列表
     */
    public List<MessageInfo> getMessageList() {
        synchronized (messageList) {
            return new ArrayList<>(messageList);
        }
    }

    public void setMsgReceiveCallback(MsgReceiveCallback msgReceiveCallback) {
        this.msgReceiveCallback = msgReceiveCallback;
    }
}
