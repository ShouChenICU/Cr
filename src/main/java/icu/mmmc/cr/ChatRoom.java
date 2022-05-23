package icu.mmmc.cr;

import icu.mmmc.cr.callbacks.MsgReceiveCallback;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.constants.Constants;
import icu.mmmc.cr.constants.MessageTypes;
import icu.mmmc.cr.database.DaoManager;
import icu.mmmc.cr.entities.MemberInfo;
import icu.mmmc.cr.entities.MessageInfo;
import icu.mmmc.cr.entities.RoomInfo;
import icu.mmmc.cr.tasks.PushTask;

import java.util.*;

/**
 * 聊天室实体
 * 包含房间详细信息，成员列表，消息列表和在线节点列表
 * 其中在线节点列表仅由房主节点维护，用以广播消息时快速索引在线节点
 *
 * @author shouchen
 */
public class ChatRoom {
    private volatile boolean isAvailable;
    private final Object availLock;
    /**
     * 房间信息
     */
    private RoomInfo roomInfo;
    /**
     * 如果该房间被自己所管理，则为true,否则为false
     */
    private final boolean isAdmin;
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
     * 消息id记录
     */
    private int maxMsgID;
    /**
     * 消息接收回调
     */
    private MsgReceiveCallback msgReceiveCallback;

    public ChatRoom(RoomInfo roomInfo) throws Exception {
        Objects.requireNonNull(roomInfo);
        roomInfo.check();
        this.roomInfo = roomInfo;
        isAvailable = true;
        isAdmin = Objects.equals(Cr.getNodeInfo().getUuid(), roomInfo.getNodeUUID());
        availLock = new Object();
        memberMap = new HashMap<>();
        onlineNodeMap = new HashMap<>();
        messageList = new ArrayList<>();
    }

    protected void disable() {
        synchronized (availLock) {
            isAvailable = false;
        }
    }

    protected void setMaxMsgID(int maxMsgID) {
        this.maxMsgID = maxMsgID;
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
    public void updateMemberInfo(MemberInfo memberInfo) throws Exception {
        synchronized (availLock) {
            if (!isAvailable) {
                throw new Exception("Room is not available");
            }
            memberInfo.check();
            if (Objects.equals(memberInfo.getNodeUUID(), roomInfo.getNodeUUID())) {
                throw new Exception("Member info illegal");
            }
            DaoManager.getMemberDao().updateMember(memberInfo);
            synchronized (memberMap) {
                memberMap.put(memberInfo.getUserUUID(), memberInfo);
            }
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

    private void broadcastMsg(MessageInfo messageInfo) throws Exception {
        synchronized (onlineNodeMap) {
            for (Node node : onlineNodeMap.values()) {
                node.addTask(new PushTask(messageInfo, null));
            }
        }
    }

    /**
     * 添加消息
     *
     * @param messageInfo 消息实体
     */
    public void putMessage(MessageInfo messageInfo) throws Exception {
        synchronized (availLock) {
            if (!isAvailable) {
                throw new Exception("Room is not available");
            }
            messageInfo.check();
            if (!Objects.equals(messageInfo.getNodeUUID(), roomInfo.getNodeUUID())
                    || !Objects.equals(messageInfo.getRoomUUID(), roomInfo.getRoomUUID())
                    || !memberMap.containsKey(messageInfo.getSenderUUID())) {
                throw new Exception("Message is illegal");
            }
            if (isAdmin) {
                messageInfo.setId(++maxMsgID);
                messageInfo.setTimestamp(System.currentTimeMillis());
                broadcastMsg(messageInfo);
            }
            DaoManager.getMessageDao().putMessage(messageInfo);
            synchronized (messageList) {
                messageList.add(messageInfo);
                messageList.sort(Comparator.comparingLong(MessageInfo::getTimestamp));
                if (messageList.size() > Constants.MSG_LIST_BUF_SIZE) {
                    messageList.remove(0);
                }
                if (msgReceiveCallback != null) {
                    msgReceiveCallback.receiveMsg(messageInfo);
                }
            }
        }
    }

    /**
     * 添加全部消息
     *
     * @param messageInfos 消息列表
     */
    protected void putMessageList(List<MessageInfo> messageInfos) throws Exception {
        synchronized (availLock) {
            if (!isAvailable) {
                throw new Exception("Room is not available");
            }
            synchronized (messageList) {
                messageList.addAll(messageInfos);
                messageList.sort(Comparator.comparingLong(MessageInfo::getTimestamp));
                while (messageList.size() > Constants.MSG_LIST_BUF_SIZE) {
                    messageList.remove(0);
                }
                if (msgReceiveCallback != null) {
                    msgReceiveCallback.receiveMsg(messageList.get(messageList.size() - 1));
                }
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
        if (!isAvailable) {
            return false;
        }
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
    public boolean containMember(String uuid) {
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
     * 发送文本消息
     *
     * @param content  文本消息内容
     * @param callback 进度回调
     */
    public void postMessage(String content, ProgressCallback callback) throws Exception {
        synchronized (availLock) {
            if (!isAvailable) {
                throw new Exception("Room is not available");
            }
            Objects.requireNonNull(content);
            if (content.length() > Constants.MAX_TEXT_MSG_LENGTH) {
                throw new Exception("Message length out of range " + Constants.MAX_TEXT_MSG_LENGTH);
            }
            MessageInfo msg = new MessageInfo()
                    .setNodeUUID(roomInfo.getNodeUUID())
                    .setRoomUUID(roomInfo.getRoomUUID())
                    .setContent(content)
                    .setSenderUUID(Cr.getNodeInfo().getUuid())
                    .setType(MessageTypes.TYPE_TEXT);
            if (isAdmin) {
                putMessage(msg);
            } else {
                Node node = NodeManager.getByUUID(roomInfo.getNodeUUID());
                if (node == null) {
                    throw new Exception("Room is offline");
                }
                node.addTask(new PushTask(msg, callback));
            }
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
