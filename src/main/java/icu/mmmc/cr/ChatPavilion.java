package icu.mmmc.cr;

import icu.mmmc.cr.callbacks.MsgReceiveCallback;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.callbacks.adapters.ProgressAdapter;
import icu.mmmc.cr.constants.*;
import icu.mmmc.cr.database.DaoManager;
import icu.mmmc.cr.entities.MemberInfo;
import icu.mmmc.cr.entities.MessageInfo;
import icu.mmmc.cr.entities.NodeInfo;
import icu.mmmc.cr.entities.RoomInfo;
import icu.mmmc.cr.exceptions.AuthenticationException;
import icu.mmmc.cr.tasks.PushTask;
import icu.mmmc.cr.tasks.RequestTask;
import icu.mmmc.cr.tasks.SyncMemberTask1;
import icu.mmmc.cr.tasks.SyncMessageTask1;
import icu.mmmc.cr.utils.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final AtomicBoolean isSyncMember;
    private final AtomicBoolean isSyncMessage;
    private int unreadCount;
    /**
     * 房间信息
     */
    private RoomInfo roomInfo;
    /**
     * 如果该房间被自己所管理，则为true,否则为false
     */
    private final boolean isOwner;
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
        isOwner = Objects.equals(Cr.getNodeInfo().getUUID(), roomInfo.getNodeUUID());
        availLock = new Object();
        memberMap = new HashMap<>();
        onlineNodeMap = new HashMap<>();
        messageList = new ArrayList<>();
        messageIdSet = new HashSet<>();
        isSyncMember = new AtomicBoolean(false);
        isSyncMessage = new AtomicBoolean(false);
        unreadCount = 0;
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
    public void updateRoomInfo(RoomInfo roomInfo) throws Exception {
        Objects.requireNonNull(roomInfo);
        synchronized (availLock) {
            if (!isAvailable) {
                throw new Exception("Room is not available");
            }
            roomInfo.check();
            if (!Objects.equals(roomInfo, this.roomInfo)) {
                throw new Exception("Room info illegal");
            }
            this.roomInfo = roomInfo;
            DaoManager.getRoomDao().updateRoomInfo(roomInfo);
        }
        if (isOwner) {
            synchronized (onlineNodeMap) {
                for (Node node : onlineNodeMap.values()) {
                    node.addTask(new PushTask(roomInfo, null));
                }
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
            if (isOwner) {
                memberInfo.setUpdateTime(System.currentTimeMillis());
                synchronized (memberMap) {
                    memberMap.put(memberInfo.getUserUUID(), memberInfo);
                    DaoManager.getMemberDao().updateMember(memberInfo);
                }
                synchronized (onlineNodeMap) {
                    for (Node node : onlineNodeMap.values()) {
                        node.addTask(new PushTask(memberInfo, null));
                    }
                }
            } else {
                synchronized (memberMap) {
                    memberMap.put(memberInfo.getUserUUID(), memberInfo);
                    DaoManager.getMemberDao().updateMember(memberInfo);
                }
            }
        }
    }

    /**
     * 添加成员
     * 主动操作，只有房主或管理员才可用
     *
     * @param UUID 成员标识码
     */
    @Override
    public void addMember(String UUID) throws Exception {
        Objects.requireNonNull(UUID);
        synchronized (availLock) {
            if (!isAvailable) {
                throw new Exception("Room is not available");
            }
            // 检查是否有权限
            String myUUID = Cr.getNodeInfo().getUUID();
            if (!isOwner) {
                // 不是房主
                synchronized ((memberMap)) {
                    MemberInfo myInfo = memberMap.get(myUUID);
                    if (myInfo == null || myInfo.getRole() != MemberRoles.ROLE_ADMIN) {
                        // 也不是管理员
                        throw new AuthenticationException("Permission deny");
                    }
                }
            }
            synchronized (memberMap) {
                if (memberMap.containsKey(UUID)) {
                    throw new Exception("Member already exists");
                }
            }
            // 是房主
            if (isOwner) {
                MemberInfo memberInfo = new MemberInfo()
                        .setNodeUUID(roomInfo.getNodeUUID())
                        .setRoomUUID(roomInfo.getRoomUUID())
                        .setUserUUID(UUID);
                NodeInfo nodeInfo = DaoManager.getNodeInfoDao().getByUUID(UUID);
                if (nodeInfo == null || nodeInfo.getAttr(NodeAttributes.$TITLE) == null) {
                    memberInfo.setNickname(UUID.substring(0, 8));
                } else {
                    memberInfo.setNickname(nodeInfo.getAttr(NodeAttributes.$TITLE));
                }
                long joinTime = System.currentTimeMillis();
                memberInfo.setUpdateTime(joinTime)
                        .setJoinTime(joinTime);
                synchronized (memberMap) {
                    memberMap.put(UUID, memberInfo);
                    DaoManager.getMemberDao().updateMember(memberInfo);
                }
                synchronized (onlineNodeMap) {
                    for (Node node : onlineNodeMap.values()) {
                        node.addTask(new PushTask(memberInfo, null));
                    }
                }
                Node node = NodeManager.getByUUID(UUID);
                if (node != null && node.isOnline()) {
                    putNode(UUID, node);
                    node.addTask(new PushTask(roomInfo, null));
                }
            } else {
                // 不是房主，但是是管理员
                Node node = NodeManager.getByUUID(roomInfo.getNodeUUID());
                if (node == null || !node.isOnline()) {
                    throw new Exception("Room is offline");
                }
                node.addTask(new RequestTask(null, RequestPaths.ADD_MEMBER, roomInfo.getRoomUUID(), UUID));
            }
        }
    }

    /**
     * 移除成员
     *
     * @param UUID 成员标识码
     */
    @Override
    public void removeMember(String UUID) throws Exception {
        Objects.requireNonNull(UUID);
        MemberInfo memberInfo;
        synchronized (availLock) {
            if (!isAvailable) {
                throw new Exception("Room is not available");
            }
            synchronized (memberMap) {
                // 检查是否有权限
                String myUUID = Cr.getNodeInfo().getUUID();
                if (!isOwner) {
                    // 不是房主
                    MemberInfo myInfo = memberMap.get(myUUID);
                    if (myInfo == null || myInfo.getRole() != MemberRoles.ROLE_ADMIN) {
                        // 也不是管理员
                        throw new AuthenticationException("Permission deny");
                    }
                }
                if (Objects.equals(UUID, roomInfo.getNodeUUID())) {
                    throw new Exception("Cannot remove homeowner");
                }
                memberInfo = memberMap.get(UUID);
                if (memberInfo == null) {
                    throw new Exception("Member already removed");
                }
            }
            if (isOwner) {
                // 如果是群主
                synchronized (memberMap) {
                    memberMap.remove(UUID);
                    DaoManager.getMemberDao()
                            .deleteMember(memberInfo.getNodeUUID(),
                                    memberInfo.getRoomUUID(),
                                    UUID);
                }
                // 推送更新
                synchronized (onlineNodeMap) {
                    for (Node node : onlineNodeMap.values()) {
                        node.addTask(new RequestTask(null, RequestPaths.DEL_MEMBER,
                                memberInfo.getNodeUUID(),
                                memberInfo.getRoomUUID(),
                                UUID));
                    }
                    onlineNodeMap.remove(UUID);
                }
            } else {
                // 不是群主，但是是管理员
                Node node = NodeManager.getByUUID(roomInfo.getNodeUUID());
                if (node == null || !node.isOnline()) {
                    throw new Exception("Node is offline");
                } else {
                    node.addTask(new RequestTask(null, RequestPaths.DEL_MEMBER,
                            memberInfo.getNodeUUID(),
                            memberInfo.getRoomUUID(),
                            UUID));
                }
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
                if (!memberMap.containsKey(userUUID)) {
                    throw new Exception("Member already removed");
                }
                memberMap.remove(userUUID);
                DaoManager.getMemberDao()
                        .deleteMember(roomInfo.getNodeUUID(),
                                roomInfo.getRoomUUID(),
                                userUUID);
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
     * 收到新的消息
     * 一般房主调用
     * 可能操作数据库
     *
     * @param messageInfo 消息实体
     */
    public void receiveMessage(MessageInfo messageInfo) throws Exception {
        Objects.requireNonNull(messageInfo);
        synchronized (availLock) {
            if (!isAvailable) {
                throw new Exception("Room is not available");
            }
            if (!Objects.equals(messageInfo.getNodeUUID(), roomInfo.getNodeUUID())
                    || !Objects.equals(messageInfo.getRoomUUID(), roomInfo.getRoomUUID())
                    || !memberMap.containsKey(messageInfo.getSenderUUID())) {
                throw new Exception("Message is illegal");
            }
            if (isOwner) {
                messageInfo.setId(++maxMsgID);
                messageInfo.setTimestamp(System.currentTimeMillis());
                broadcastMsg(messageInfo);
            }
            synchronized (messageList) {
                if (messageIdSet.add(messageInfo.getId())) {
                    messageList.add(messageInfo);
                    DaoManager.getMessageDao().putMessage(messageInfo);
                    unreadCount++;
                    messageList.sort(Comparator.comparingLong(MessageInfo::getTimestamp));
                    if (messageList.size() > Constants.MSG_LIST_BUF_SIZE) {
                        messageIdSet.remove(messageList.remove(0).getId());
                    }
                    if (msgReceiveCallback != null) {
                        msgReceiveCallback.receiveMsg(this, messageInfo);
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
                    || !Objects.equals(messageInfo.getRoomUUID(), roomInfo.getRoomUUID())) {
                throw new Exception("Message is illegal");
            }
            synchronized (messageList) {
                if (messageIdSet.add(messageInfo.getId())) {
                    messageList.add(messageInfo);
                    unreadCount++;
                    messageList.sort(Comparator.comparingLong(MessageInfo::getTimestamp));
                    if (messageList.size() > Constants.MSG_LIST_BUF_SIZE) {
                        messageIdSet.remove(messageList.remove(0).getId());
                    }
                    if (msgReceiveCallback != null) {
                        msgReceiveCallback.receiveMsg(this, messageInfo);
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
                for (MessageInfo info : messageList) {
                    messageIdSet.add(info.getId());
                }
                messageList.sort(Comparator.comparingLong(MessageInfo::getTimestamp));
                while (messageList.size() > Constants.MSG_LIST_BUF_SIZE) {
                    messageIdSet.remove(messageList.remove(0).getId());
                }
                if (msgReceiveCallback != null) {
                    msgReceiveCallback.receiveMsg(this, messageList.get(messageList.size() - 1));
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
        if (Objects.equals(Cr.getNodeInfo().getUUID(), roomInfo.getNodeUUID())) {
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
     * 获取成员数量
     *
     * @return 成员数量
     */
    @Override
    public int getMemberCount() {
        return memberMap.size();
    }

    /**
     * 更新自己的昵称
     *
     * @param nickname 昵称
     */
    @Override
    public void updateNickname(String nickname) throws Exception {
        Objects.requireNonNull(nickname);
        synchronized (availLock) {
            if (!isAvailable) {
                throw new Exception("Room is not available");
            }
            String UUID = Cr.getNodeInfo().getUUID();
            MemberInfo memberInfo = getMemberInfo(UUID);
            if (memberInfo == null) {
                throw new Exception("Member not found");
            }
            if (nickname.length() > Constants.MAX_NICKNAME_LENGTH) {
                throw new Exception("Nickname length out of range " + Constants.MAX_NICKNAME_LENGTH);
            }
            if (isOwner) {
                memberInfo.setNickname(nickname)
                        .setUpdateTime(System.currentTimeMillis());
                DaoManager.getMemberDao().updateMember(memberInfo);
                synchronized (onlineNodeMap) {
                    for (Node node : onlineNodeMap.values()) {
                        node.addTask(new PushTask(memberInfo, null));
                    }
                }
            } else {
                Node node = NodeManager.getByUUID(roomInfo.getNodeUUID());
                if (node == null || !node.isOnline()) {
                    throw new Exception("Node is offline");
                }
                node.addTask(new RequestTask(null,
                        RequestPaths.UPDATE_NICKNAME,
                        nickname,
                        roomInfo.getRoomUUID()));
            }
        }
    }

    /**
     * 更新自己的头衔
     *
     * @param label 头衔
     */
    @Override
    public void updateLabel(String label) throws Exception {
        Objects.requireNonNull(label);
        synchronized (availLock) {
            if (!isAvailable) {
                throw new Exception("Room is not available");
            }
            String UUID = Cr.getNodeInfo().getUUID();
            MemberInfo memberInfo = getMemberInfo(UUID);
            if (memberInfo == null) {
                throw new Exception("Member not found");
            }
            if (label.length() > Constants.MAX_LABEL_LENGTH) {
                throw new Exception("Label length out of range " + Constants.MAX_LABEL_LENGTH);
            }
            if (isOwner) {
                memberInfo.setLabel(label)
                        .setUpdateTime(System.currentTimeMillis());
                DaoManager.getMemberDao().updateMember(memberInfo);
                synchronized (onlineNodeMap) {
                    for (Node node : onlineNodeMap.values()) {
                        node.addTask(new PushTask(memberInfo, null));
                    }
                }
            } else {
                Node node = NodeManager.getByUUID(roomInfo.getNodeUUID());
                if (node == null || !node.isOnline()) {
                    throw new Exception("Node is offline");
                }
                node.addTask(new RequestTask(null,
                        RequestPaths.UPDATE_LABEL,
                        label,
                        roomInfo.getRoomUUID()));
            }
        }
    }

    /**
     * 更新房间名称
     *
     * @param title 房间名
     */
    @Override
    public void updateRoomTitle(String title) throws Exception {
        Objects.requireNonNull(title);
        synchronized (availLock) {
            if (!isAvailable) {
                throw new Exception("Room is not available");
            }
            // 检查是否有权限
            String myUUID = Cr.getNodeInfo().getUUID();
            if (!isOwner) {
                // 不是房主
                MemberInfo myInfo = memberMap.get(myUUID);
                if (myInfo == null || myInfo.getRole() != MemberRoles.ROLE_ADMIN) {
                    // 也不是管理员
                    throw new AuthenticationException("Permission deny");
                }
            }
            if (title.length() > Constants.MAX_ROOM_TITLE_LENGTH) {
                throw new Exception("Title out of range " + Constants.MAX_ROOM_TITLE_LENGTH);
            }
            if (isOwner) {
                roomInfo.setTitle(title)
                        .setUpdateTime(System.currentTimeMillis());
                updateRoomInfo(roomInfo);
            } else {
                // 不是群主，但是是管理员
                Node node = NodeManager.getByUUID(roomInfo.getNodeUUID());
                if (node == null || !node.isOnline()) {
                    throw new Exception("Node is offline");
                } else {
                    node.addTask(new RequestTask(null, RequestPaths.UPDATE_ROOM_TITLE,
                            title,
                            roomInfo.getRoomUUID()));
                }
            }
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
        if (uuid == null) {
            return null;
        }
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
        if (uuid == null) {
            return false;
        }
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
     * 获取最新消息
     *
     * @return 最新消息
     */
    @Override
    public MessageInfo getLatestMessage() {
        synchronized (messageList) {
            if (messageList.isEmpty()) {
                return null;
            } else {
                return messageList.get(messageList.size() - 1);
            }
        }
    }

    /**
     * 获取未读消息数量
     *
     * @return 未读消息数
     */
    @Override
    public int getUnreadCount() {
        return unreadCount;
    }

    /**
     * 设置未读消息数量
     *
     * @param unreadCount 未读消息数量
     */
    @Override
    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    /**
     * 发送文本消息
     *
     * @param content  文本消息内容
     * @param callback 进度回调
     */
    @Override
    public void postMessage(String content, ProgressCallback callback) {
        if (callback == null) {
            callback = new ProgressAdapter();
        }
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
                        .setSenderUUID(Cr.getNodeInfo().getUUID())
                        .setType(MessageTypes.TYPE_TEXT);
                if (isOwner) {
                    receiveMessage(msg);
                    callback.done(null);
                } else {
                    Node node = NodeManager.getByUUID(roomInfo.getNodeUUID());
                    if (node == null || !node.isOnline()) {
                        throw new Exception("Room is offline");
                    }
                    ProgressCallback finalCallback = callback;
                    node.addTask(new RequestTask(
                            new ProgressAdapter() {
                                @Override
                                public void done(Object result) {
                                    finalCallback.done(result);
                                }

                                @Override
                                public void halt(String msg) {
                                    finalCallback.halt(msg);
                                }
                            },
                            RequestPaths.SEND_TEXT_MSG,
                            roomInfo.getRoomUUID(),
                            msg));
                }
            }
        } catch (Exception e) {
            Logger.warn(e);
            callback.halt(Objects.requireNonNullElse(e.getMessage(), e.toString()));
        }
    }

    /**
     * 授权管理员权限
     *
     * @param userUUID 成员标识码
     */
    @Override
    public void grantAdminPermission(String userUUID) throws Exception {
        Objects.requireNonNull(userUUID);
        synchronized (availLock) {
            if (!isAvailable) {
                throw new Exception("Room is not available");
            }
            if (!isOwner) {
                throw new AuthenticationException("Permission deny");
            }
            MemberInfo memberInfo = getMemberInfo(userUUID);
            if (memberInfo == null) {
                throw new Exception("Member not found");
            }
            if (memberInfo.getRole() == MemberRoles.ROLE_ADMIN
                    || memberInfo.getRole() == MemberRoles.ROLE_OWNER) {
                return;
            }
            memberInfo.setRole(MemberRoles.ROLE_ADMIN)
                    .setUpdateTime(System.currentTimeMillis());
            DaoManager.getMemberDao().updateMember(memberInfo);
            synchronized (onlineNodeMap) {
                for (Node node : onlineNodeMap.values()) {
                    node.addTask(new PushTask(memberInfo, null));
                }
            }
        }
    }

    /**
     * 撤销管理员权限
     *
     * @param userUUID 成员标识码
     */
    @Override
    public void revokeAdminPermission(String userUUID) throws Exception {
        Objects.requireNonNull(userUUID);
        synchronized (availLock) {
            if (!isAvailable) {
                throw new Exception("Room is not available");
            }
            if (!isOwner) {
                throw new AuthenticationException("Permission deny");
            }
            MemberInfo memberInfo = getMemberInfo(userUUID);
            if (memberInfo == null) {
                throw new Exception("Member not found");
            }
            if (memberInfo.getRole() == MemberRoles.ROLE_NORMAL
                    || memberInfo.getRole() == MemberRoles.ROLE_OWNER) {
                return;
            }
            memberInfo.setRole(MemberRoles.ROLE_NORMAL)
                    .setUpdateTime(System.currentTimeMillis());
            DaoManager.getMemberDao().updateMember(memberInfo);
            synchronized (onlineNodeMap) {
                for (Node node : onlineNodeMap.values()) {
                    node.addTask(new PushTask(memberInfo, null));
                }
            }
        }
    }

    /**
     * 同步指定时间之前的消息列表
     *
     * @param earliestTime 时间
     * @param callback     回调
     */
    @Override
    public void syncMessagesBeforeTime(long earliestTime, ProgressCallback callback) {
        if (callback == null) {
            callback = new ProgressAdapter();
        }
        try {
            synchronized (availLock) {
                if (!isAvailable) {
                    throw new Exception("Room is not available");
                } else if (isOwner) {
                    callback.done(null);
                    return;
                }
                ProgressCallback finalCallback = callback;
                SyncMessageTask1 syncMessageTask1 = new SyncMessageTask1(this, earliestTime, new ProgressCallback() {
                    @Override
                    public void start() {
                        finalCallback.start();
                    }

                    @Override
                    public void update(double status, String msg) {
                        finalCallback.update(status, msg);
                    }

                    @Override
                    public void done(Object result) {
                        finalCallback.done(result);
                        isSyncMessage.set(false);
                    }

                    @Override
                    public void halt(String msg) {
                        finalCallback.halt(msg);
                        isSyncMessage.set(false);
                    }
                });
                Node node = NodeManager.getByUUID(roomInfo.getNodeUUID());
                if (node == null || !node.isOnline()) {
                    callback.halt("Node is offline");
                    return;
                }
                if (!isSyncMessage.getAndSet(true)) {
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
        if (callback == null) {
            callback = new ProgressAdapter();
        }
        try {
            synchronized (availLock) {
                if (!isAvailable) {
                    throw new Exception("Room is not available");
                } else if (isOwner) {
                    callback.done(null);
                    return;
                }
                Node node = NodeManager.getByUUID(roomInfo.getNodeUUID());
                if (node == null || !node.isOnline()) {
                    callback.halt("Node is offline");
                    return;
                }
                if (!isSyncMember.getAndSet(true)) {
                    ProgressCallback finalCallback = callback;
                    node.addTask(new SyncMemberTask1(this, new ProgressCallback() {
                        @Override
                        public void start() {
                            finalCallback.start();
                        }

                        @Override
                        public void update(double status, String msg) {
                            finalCallback.update(status, msg);
                        }

                        @Override
                        public void done(Object result) {
                            finalCallback.done(result);
                            isSyncMember.set(false);
                        }

                        @Override
                        public void halt(String msg) {
                            finalCallback.halt(msg);
                            isSyncMember.set(false);
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

    @Override
    public boolean isOwner() {
        return isOwner;
    }
}
