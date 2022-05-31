package icu.mmmc.cr;

import icu.mmmc.cr.callbacks.MsgReceiveCallback;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.constants.Constants;
import icu.mmmc.cr.constants.MessageTypes;
import icu.mmmc.cr.constants.NodeAttributes;
import icu.mmmc.cr.database.DaoManager;
import icu.mmmc.cr.entities.MemberInfo;
import icu.mmmc.cr.entities.MessageInfo;
import icu.mmmc.cr.entities.NodeInfo;
import icu.mmmc.cr.entities.RoomInfo;
import icu.mmmc.cr.tasks.PushTask;
import icu.mmmc.cr.tasks.SyncMemberTask1;
import icu.mmmc.cr.tasks.SyncMessageTask1;
import icu.mmmc.cr.utils.Logger;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 聊天室实现
 * 包含房间详细信息，成员列表，消息列表和在线节点列表
 * 其中在线节点列表仅由房主节点维护，用以广播消息时快速索引在线节点
 *
 * @author shouchen
 */
@SuppressWarnings("DuplicatedCode")
public class ChatPavilion implements ChatRoom {
    private volatile boolean isAvailable;
    private final Object availLock;
    private final ReentrantLock syncMemberLock;
    private final ReentrantLock syncMessageLock;
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
     * 消息去重集合
     */
    private final Set<Integer> messageIdSet;
    /**
     * 消息id记录
     */
    private int maxMsgID;
    /**
     * 消息接收回调
     */
    private MsgReceiveCallback msgReceiveCallback;

    public ChatPavilion(RoomInfo roomInfo) throws Exception {
        Objects.requireNonNull(roomInfo);
        roomInfo.check();
        this.roomInfo = roomInfo;
        isAvailable = true;
        isAdmin = Objects.equals(Cr.getNodeInfo().getUuid(), roomInfo.getNodeUUID());
        availLock = new Object();
        memberMap = new HashMap<>();
        onlineNodeMap = new HashMap<>();
        messageList = new ArrayList<>();
        messageIdSet = new HashSet<>();
        syncMemberLock = new ReentrantLock();
        syncMessageLock = new ReentrantLock();
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
    @Override
    public RoomInfo getRoomInfo() {
        return roomInfo;
    }

    /**
     * 更新房间信息
     *
     * @param roomInfo 房间信息
     */
    @Override
    public void updateRoomInfo(RoomInfo roomInfo) throws Exception {
        Objects.requireNonNull(roomInfo);
        synchronized (availLock) {
            if (!isAvailable) {
                throw new Exception("Room is not available");
            }
            if (!isAdmin) {
                throw new Exception("Not the homeowner");
            }
            roomInfo.check();
            if (!Objects.equals(roomInfo, this.roomInfo)) {
                throw new Exception("Room info illegal");
            }
            this.roomInfo = roomInfo;
        }
        synchronized (onlineNodeMap) {
            for (Node node : onlineNodeMap.values()) {
                node.addTask(new PushTask(roomInfo, null));
            }
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
            if (!Objects.equals(memberInfo.getNodeUUID(), roomInfo.getNodeUUID())
                    || !Objects.equals(memberInfo.getRoomUUID(), roomInfo.getRoomUUID())) {
                throw new Exception("Member info illegal");
            }
            synchronized (memberMap) {
                memberMap.put(memberInfo.getUserUUID(), memberInfo);
            }
            DaoManager.getMemberDao().updateMember(memberInfo);
        }
        if (isAdmin) {
            synchronized (onlineNodeMap) {
                for (Node node : onlineNodeMap.values()) {
                    node.addTask(new PushTask(memberInfo, null));
                }
            }
        }
    }

    /**
     * 更新成员
     *
     * @param memberInfo 成员信息
     */
    @Override
    public void updateMember(MemberInfo memberInfo) throws Exception {
        Objects.requireNonNull(memberInfo);
        synchronized (availLock) {
            if (!isAvailable) {
                throw new Exception("Room is not available");
            }
            memberInfo.check();
            if (!Objects.equals(memberInfo.getNodeUUID(), roomInfo.getNodeUUID())
                    || !Objects.equals(memberInfo.getRoomUUID(), roomInfo.getRoomUUID())) {
                throw new Exception("Member info illegal");
            }
            if (isAdmin) {
                updateMemberInfo(memberInfo);
            } else {
                Node node = NodeManager.getByUUID(roomInfo.getNodeUUID());
                if (node == null || !node.isOnline()) {
                    throw new Exception("Room is offline");
                }
                node.addTask(new PushTask(memberInfo, null));
            }
        }
    }

    /**
     * 添加成员
     * 主动操作，只有房主或管理员才可用
     *
     * @param memberInfo 成员信息
     */
    @Override
    public void addMember(MemberInfo memberInfo) throws Exception {
        Objects.requireNonNull(memberInfo);
        synchronized (availLock) {
            if (!isAvailable) {
                throw new Exception("Room is not available");
            }
            // 检查是否是房主
            if (!Objects.equals(Cr.getNodeInfo().getUuid(), roomInfo.getNodeUUID())) {
                throw new Exception("Not the homeowner");
            }
            if (memberInfo.getUserUUID() == null) {
                throw new Exception("User uuid is null");
            } else if (memberInfo.getNickname() == null) {
                NodeInfo nodeInfo = DaoManager.getNodeInfoDao().getByUUID(memberInfo.getUserUUID());
                if (nodeInfo == null || nodeInfo.getAttr(NodeAttributes.$TITLE) == null) {
                    memberInfo.setNickname(memberInfo.getUserUUID());
                } else {
                    memberInfo.setNickname(nodeInfo.getAttr(NodeAttributes.$TITLE));
                }
            }
            if (memberInfo.getNickname().length() > Constants.MAX_NICKNAME_LENGTH) {
                throw new Exception("Nickname length out of range " + Constants.MAX_NICKNAME_LENGTH);
            }
            memberInfo.setNodeUUID(roomInfo.getNodeUUID())
                    .setRoomUUID(roomInfo.getRoomUUID())
                    .setUpdateTime(System.currentTimeMillis());
            synchronized (memberMap) {
                if (memberMap.containsKey(memberInfo.getUserUUID())) {
                    throw new Exception("Member already exists");
                }
                memberMap.put(memberInfo.getUserUUID(), memberInfo);
                DaoManager.getMemberDao().updateMember(memberInfo);
            }
        }
        // 推送新成员
        synchronized (onlineNodeMap) {
            for (Node node : onlineNodeMap.values()) {
                node.addTask(new PushTask(memberInfo, null));
            }
        }
    }

    /**
     * 移除成员
     *
     * @param memberInfo 成员信息
     */
    @Override
    public void removeMember(MemberInfo memberInfo) throws Exception {
        Objects.requireNonNull(memberInfo);
        synchronized (availLock) {
            if (!isAvailable) {
                throw new Exception("Room is not available");
            }
            memberInfo.check();
            if (!Objects.equals(memberInfo.getNodeUUID(), roomInfo.getNodeUUID())
                    || !Objects.equals(memberInfo.getRoomUUID(), roomInfo.getRoomUUID())) {
                throw new Exception("Member info illegal");
            }
            // 检查是否是房主
            if (!Objects.equals(Cr.getNodeInfo().getUuid(), roomInfo.getNodeUUID())) {
                throw new Exception("Not the homeowner");
            }
            synchronized (memberMap) {
                if (memberMap.containsKey(memberInfo.getUserUUID())) {
                    throw new Exception("Member already removed");
                }
                memberMap.remove(memberInfo.getUserUUID());
                DaoManager.getMemberDao().deleteMember(memberInfo);
            }
        }
        // 推送更新
        synchronized (onlineNodeMap) {
            for (Node node : onlineNodeMap.values()) {
                // TODO: 2022/5/28  
            }
        }
    }

    /**
     * 删除成员
     *
     * @param userUUID 成员标识码
     */
    public void deleteMember(String userUUID) throws Exception {
        Objects.requireNonNull(userUUID);
        synchronized (availLock) {
            if (!isAvailable) {
                throw new Exception("Room is not available");
            }
            synchronized (memberMap) {
                if (memberMap.containsKey(userUUID)) {
                    throw new Exception("Member already removed");
                }
                memberMap.remove(userUUID);
                DaoManager.getMemberDao().deleteMember(new MemberInfo()
                        .setNodeUUID(roomInfo.getNodeUUID())
                        .setRoomUUID(roomInfo.getRoomUUID())
                        .setUserUUID(userUUID));
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

    /**
     * 广播消息
     *
     * @param messageInfo 消息实体
     * @throws Exception 异常
     */
    private void broadcastMsg(MessageInfo messageInfo) throws Exception {
        synchronized (onlineNodeMap) {
            for (Node node : onlineNodeMap.values()) {
                node.addTask(new PushTask(messageInfo, null));
            }
        }
    }

    /**
     * 接收消息
     * 可能操作数据库
     *
     * @param messageInfo 消息实体
     */
    public void receiveMessage(MessageInfo messageInfo) throws Exception {
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
            synchronized (messageList) {
                if (messageIdSet.add(messageInfo.getId())) {
                    messageList.add(messageInfo);
                    messageList.sort(Comparator.comparingLong(MessageInfo::getTimestamp));
                    if (messageList.size() > Constants.MSG_LIST_BUF_SIZE) {
                        messageIdSet.remove(messageList.remove(0).getId());
                    }
                    DaoManager.getMessageDao().putMessage(messageInfo);
                    if (msgReceiveCallback != null) {
                        msgReceiveCallback.receiveMsg(messageInfo);
                    }
                }
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
            synchronized (messageList) {
                if (messageIdSet.add(messageInfo.getId())) {
                    messageList.add(messageInfo);
                    messageList.sort(Comparator.comparingLong(MessageInfo::getTimestamp));
                    if (messageList.size() > Constants.MSG_LIST_BUF_SIZE) {
                        messageIdSet.remove(messageList.remove(0).getId());
                    }
                    if (msgReceiveCallback != null) {
                        msgReceiveCallback.receiveMsg(messageInfo);
                    }
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
    @Override
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
    @Override
    public List<MemberInfo> getMemberList() {
        synchronized (memberMap) {
            return new ArrayList<>(memberMap.values());
        }
    }

    /**
     * 获取成员信息
     *
     * @param uuid 成员标识码
     * @return 成员信息，没有则返回null
     */
    @Override
    public MemberInfo getMemberInfo(String uuid) {
        synchronized (memberMap) {
            return memberMap.get(uuid);
        }
    }

    /**
     * 是否存在成员
     *
     * @param uuid 成员标识码
     * @return 如果存在该成员则返回true, 否则返回false
     */
    @Override
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
    @Override
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
    @Override
    public void postMessage(String content, ProgressCallback callback) {
        try {
            synchronized (availLock) {
                if (!isAvailable) {
                    throw new Exception("Room is not available");
                }
                Objects.requireNonNull(content);
                if (content.length() > Constants.MAX_MSG_PAYLOAD_LENGTH) {
                    throw new Exception("Message length out of range " + Constants.MAX_MSG_PAYLOAD_LENGTH);
                }
                MessageInfo msg = new MessageInfo()
                        .setNodeUUID(roomInfo.getNodeUUID())
                        .setRoomUUID(roomInfo.getRoomUUID())
                        .setContent(content)
                        .setSenderUUID(Cr.getNodeInfo().getUuid())
                        .setType(MessageTypes.TYPE_TEXT);
                if (isAdmin) {
                    receiveMessage(msg);
                } else {
                    Node node = NodeManager.getByUUID(roomInfo.getNodeUUID());
                    if (node == null || !node.isOnline()) {
                        throw new Exception("Room is offline");
                    }
                    node.addTask(new PushTask(msg, callback));
                }
            }
        } catch (Exception e) {
            Logger.warn(e);
            callback.halt(Objects.requireNonNullElse(e.getMessage(), e.toString()));
        }
    }

    /**
     * 同步指定时间之前的消息列表
     *
     * @param timeStamp 时间戳
     */
    @Override
    public void syncMessagesBeforeTime(long timeStamp, ProgressCallback callback) {
        try {
            synchronized (availLock) {
                if (!isAvailable) {
                    throw new Exception("Room is not available");
                } else if (isAdmin) {
                    callback.done();
                    return;
                }
                SyncMessageTask1 syncMessageTask1 = new SyncMessageTask1(this, timeStamp, new ProgressCallback() {
                    @Override
                    public void start() {
                        callback.start();
                    }

                    @Override
                    public void update(double status, String msg) {
                        callback.update(status, msg);
                    }

                    @Override
                    public void done() {
                        callback.done();
                        syncMessageLock.unlock();
                    }

                    @Override
                    public void halt(String msg) {
                        callback.halt(msg);
                        syncMessageLock.unlock();
                    }
                });
                Node node = NodeManager.getByUUID(roomInfo.getNodeUUID());
                if (node == null || !node.isOnline()) {
                    callback.halt("Node is offline");
                    return;
                }
                if (syncMessageLock.tryLock()) {
                    node.addTask(syncMessageTask1);
                }
            }
        } catch (Exception e) {
            Logger.warn(e);
            callback.halt(Objects.requireNonNullElse(e.getMessage(), e.toString()));
        }
    }

    /**
     * 同步成员列表
     */
    @Override
    public void syncMembers(ProgressCallback callback) {
        try {
            synchronized (availLock) {
                if (!isAvailable) {
                    throw new Exception("Room is not available");
                } else if (isAdmin) {
                    callback.done();
                    return;
                }
                Node node = NodeManager.getByUUID(roomInfo.getNodeUUID());
                if (node == null || !node.isOnline()) {
                    callback.halt("Node is offline");
                    return;
                }
                if (syncMemberLock.tryLock()) {
                    node.addTask(new SyncMemberTask1(this, new ProgressCallback() {
                        @Override
                        public void start() {
                            callback.start();
                        }

                        @Override
                        public void update(double status, String msg) {
                            callback.update(status, msg);
                        }

                        @Override
                        public void done() {
                            callback.done();
                            syncMemberLock.unlock();
                        }

                        @Override
                        public void halt(String msg) {
                            callback.halt(msg);
                            syncMemberLock.unlock();
                        }
                    }));
                }
            }
        } catch (Exception e) {
            Logger.warn(e);
            callback.halt(Objects.requireNonNullElse(e.getMessage(), e.toString()));
        }
    }

    /**
     * 设置消息接收回调
     *
     * @param msgReceiveCallback 消息接收回调
     */
    @Override
    public void setMsgReceiveCallback(MsgReceiveCallback msgReceiveCallback) {
        this.msgReceiveCallback = msgReceiveCallback;
    }
}
