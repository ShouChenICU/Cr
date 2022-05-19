package icu.mmmc.cr;

import icu.mmmc.cr.callbacks.MsgReceiveCallback;
import icu.mmmc.cr.entities.MemberInfo;
import icu.mmmc.cr.entities.MessageInfo;
import icu.mmmc.cr.entities.RoomInfo;

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
    public static final int MSG_LIST_BUF_SIZE = 20;
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
    /**
     * 消息接收回调
     */
    private MsgReceiveCallback msgReceiveCallback;

    public ChatRoom(RoomInfo roomInfo) throws Exception {
        Objects.requireNonNull(roomInfo);
        roomInfo.check();
        this.roomInfo = roomInfo;
        this.memberMap = new HashMap<>();
        onlineNodeMap = new HashMap<>();
        messageList = new ArrayList<>(MSG_LIST_BUF_SIZE + 1);
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
    protected void updateRoomInfo(RoomInfo roomInfo) throws Exception {
        if (roomInfo == null) {
            return;
        }
        roomInfo.check();
        if (!Objects.equals(roomInfo, this.roomInfo)) {
            return;
        }
        synchronized (this) {
            this.roomInfo = roomInfo;
        }
    }

    /**
     * 更新成员信息
     *
     * @param memberInfo 成员信息
     */
    protected void updateMemberInfo(MemberInfo memberInfo) {
        synchronized (memberMap) {
            memberMap.put(memberInfo.getUserUUID(), memberInfo);
        }
    }

    /**
     * 设置成员列表
     *
     * @param memberInfoList 成员信息列表
     */
    protected void setMemberList(List<MemberInfo> memberInfoList) {
        synchronized (memberMap) {
            memberMap.clear();
            for (MemberInfo info : memberInfoList) {
                memberMap.put(info.getUserUUID(), info);
            }
        }
    }

    /**
     * 添加在线节点
     *
     * @param uuid 节点标识码
     * @param node 节点实体
     */
    protected void putNode(String uuid, Node node) {
        synchronized (onlineNodeMap) {
            onlineNodeMap.put(uuid, node);
        }
    }

    /**
     * 移除节点
     *
     * @param uuid 节点标识码
     */
    protected void removeNode(String uuid) {
        synchronized (onlineNodeMap) {
            onlineNodeMap.remove(uuid);
        }
    }

    /**
     * 添加消息
     *
     * @param messageInfo 消息实体
     */
    protected void putMessage(MessageInfo messageInfo) {
        synchronized (messageList) {
            messageList.add(messageInfo);
            messageList.sort(Comparator.comparingLong(MessageInfo::getTimestamp));
            if (messageList.size() > MSG_LIST_BUF_SIZE) {
                messageList.remove(0);
            }
            if (msgReceiveCallback != null) {
                msgReceiveCallback.receiveMsg(messageInfo);
            }
        }
    }

    /**
     * 添加全部消息
     *
     * @param messageInfos 消息列表
     */
    protected void putMessages(List<MessageInfo> messageInfos) {
        synchronized (messageList) {
            messageList.addAll(messageInfos);
            messageList.sort(Comparator.comparingLong(MessageInfo::getTimestamp));
            while (messageList.size() > MSG_LIST_BUF_SIZE) {
                messageList.remove(0);
            }
            if (msgReceiveCallback != null) {
                msgReceiveCallback.receiveMsg(messageList.get(messageList.size() - 1));
            }
        }
    }

    /**
     * 是否在线
     *
     * @return 如果房间主人是自己，直接返回true
     * 如果房间主人在线，则返回true
     * 否则返回false
     */
    public boolean isOnline() {
        if (Objects.equals(Cr.getNodeInfo().getUuid(), roomInfo.getNodeUUID())) {
            return true;
        }
        Node node = NodeManager.getByUUID(roomInfo.getNodeUUID());
        return node != null && node.isOnline();
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
     * 获取消息列表
     *
     * @return 消息列表
     */
    public List<MessageInfo> getMessageList() {
        synchronized (messageList) {
            return new ArrayList<>(messageList);
        }
    }

    /**
     * 设置消息接收回调
     *
     * @param msgReceiveCallback 消息接收回调
     */
    public void setMsgReceiveCallback(MsgReceiveCallback msgReceiveCallback) {
        this.msgReceiveCallback = msgReceiveCallback;
    }
}
