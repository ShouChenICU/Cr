package icu.mmmc.cr;

import icu.mmmc.cr.constants.Constants;
import icu.mmmc.cr.constants.MemberRoles;
import icu.mmmc.cr.constants.NodeAttributes;
import icu.mmmc.cr.database.DaoManager;
import icu.mmmc.cr.database.interfaces.MemberDao;
import icu.mmmc.cr.entities.MemberInfo;
import icu.mmmc.cr.entities.NodeInfo;
import icu.mmmc.cr.entities.RoomInfo;
import icu.mmmc.cr.utils.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天室管理器
 * 管理全局聊天室
 * 维护一个全部聊天室列表和自己创建的房间map
 *
 * @author shouchen
 */
public final class ChatRoomManager {
    /**
     * 全部聊天室列表
     */
    private static final List<ChatPavilion> CHAT_ROOM_LIST;
    /**
     * 被管理的聊天室map(房主是自己)
     */
    private static final Map<String, ChatPavilion> MANAGE_ROOM_MAP;

    static {
        CHAT_ROOM_LIST = new ArrayList<>();
        MANAGE_ROOM_MAP = new HashMap<>();
    }

    /**
     * 注册节点到被管理的房间
     *
     * @param node 节点
     */
    static void registerNode(Node node) {
        if (node == null || node.getNodeInfo() == null || node.getNodeInfo().getUUID() == null) {
            Logger.warn("Node info broken");
            return;
        }
        String uuid = node.getNodeInfo().getUUID();
        Logger.debug("Register node " + uuid);
        synchronized (MANAGE_ROOM_MAP) {
            for (ChatPavilion chatPavilion : MANAGE_ROOM_MAP.values()) {
                if (chatPavilion.containMember(uuid)) {
                    chatPavilion.putNode(uuid, node);
                }
            }
        }
        ConcurrentHashMap<String, ChatRoom> roomMap = node.getRoomMap();
        synchronized (CHAT_ROOM_LIST) {
            for (ChatPavilion chatPavilion : CHAT_ROOM_LIST) {
                if (Objects.equals(chatPavilion.getRoomInfo().getNodeUUID(), uuid)) {
                    roomMap.put(chatPavilion.getRoomInfo().getRoomUUID(), chatPavilion);
                }
            }
        }
    }

    /**
     * 注销节点
     *
     * @param uuid 节点标识码
     */
    static void unregisterNode(String uuid) {
        if (uuid == null) {
            Logger.warn("UUID is null");
            return;
        }
        Logger.debug("Unregister node " + uuid);
        synchronized (MANAGE_ROOM_MAP) {
            for (ChatPavilion chatPavilion : MANAGE_ROOM_MAP.values()) {
                chatPavilion.removeNode(uuid);
            }
        }
    }

    /**
     * 更新房间信息
     *
     * @param roomInfo 房间信息
     */
    public static void updateRoomInfo(RoomInfo roomInfo) {
        try {
            roomInfo.check();
            synchronized (CHAT_ROOM_LIST) {
                for (ChatPavilion chatPavilion : CHAT_ROOM_LIST) {
                    if (Objects.equals(roomInfo, chatPavilion.getRoomInfo())) {
                        chatPavilion.updateRoomInfo(roomInfo);
                        Logger.debug("Update room info. node:" + roomInfo.getNodeUUID() + " room:" + roomInfo.getRoomUUID());
                        DaoManager.getRoomDao().updateRoomInfo(roomInfo);
                        chatPavilion.syncMembers(null);
                        chatPavilion.syncMessagesBeforeTime(System.currentTimeMillis(), null);
                        Objects.requireNonNullElse(Cr.CallBack.chatRoomUpdateCallback, () -> {
                        }).update();
                        return;
                    }
                }
                // 新房间
                // TODO: 2022/5/31 新房间加入逻辑需要优化
                if (Objects.requireNonNullElse(Cr.CallBack.joinNewRoomCallback, r -> false).joinNewRoom(roomInfo)) {
                    ChatPavilion chatPavilion = new ChatPavilion(roomInfo);
                    CHAT_ROOM_LIST.add(chatPavilion);
                    Node node = NodeManager.getByUUID(roomInfo.getNodeUUID());
                    if (node != null) {
                        node.getRoomMap().put(roomInfo.getRoomUUID(), chatPavilion);
                    }
                    chatPavilion.syncMembers(null);
                    chatPavilion.syncMessagesBeforeTime(System.currentTimeMillis(), null);
                    Logger.debug("Add a new room info. node:" + roomInfo.getNodeUUID() + " room:" + roomInfo.getRoomUUID());
                    Objects.requireNonNullElse(Cr.CallBack.chatRoomUpdateCallback, () -> {
                    }).update();
                }
            }
            DaoManager.getRoomDao().updateRoomInfo(roomInfo);
        } catch (Exception e) {
            Logger.warn(e);
        }
    }

    /**
     * 创建聊天室
     *
     * @param nodeUUID 所属节点标识码
     */
    static synchronized ChatRoom createChatRoom(String nodeUUID, String title) throws Exception {
        Objects.requireNonNull(title);
        if (title.length() > Constants.MAX_ROOM_TITLE_LENGTH) {
            throw new Exception("Title length out of range " + Constants.MAX_ROOM_TITLE_LENGTH);
        }
        String roomUUID = UUID.randomUUID().toString();
        ChatPavilion chatPavilion;
        synchronized (MANAGE_ROOM_MAP) {
            while (MANAGE_ROOM_MAP.get(roomUUID) != null) {
                roomUUID = UUID.randomUUID().toString();
            }
            RoomInfo roomInfo = new RoomInfo()
                    .setNodeUUID(nodeUUID)
                    .setRoomUUID(roomUUID)
                    .setTitle(title)
                    .setUpdateTime(System.currentTimeMillis());
            DaoManager.getRoomDao().updateRoomInfo(roomInfo);
            chatPavilion = new ChatPavilion(roomInfo);
            long joinTime = System.currentTimeMillis();
            chatPavilion.updateMemberInfo(new MemberInfo()
                    .setNodeUUID(nodeUUID)
                    .setRoomUUID(roomUUID)
                    .setUserUUID(nodeUUID)
                    .setNickname(Cr.getNodeInfo().getAttr(NodeAttributes.$TITLE))
                    .setLabel("")
                    .setRole(MemberRoles.ROLE_OWNER)
                    .setUpdateTime(joinTime)
                    .setJoinTime(joinTime));
            MANAGE_ROOM_MAP.put(roomUUID, chatPavilion);
            synchronized (CHAT_ROOM_LIST) {
                CHAT_ROOM_LIST.add(chatPavilion);
            }
        }
        Objects.requireNonNullElse(Cr.CallBack.chatRoomUpdateCallback, () -> {
        }).update();
        return chatPavilion;
    }

    /**
     * 删除聊天室
     *
     * @param nodeUUID 节点标识码
     * @param roomUUID 房间标识码
     * @throws Exception 异常
     */
    static void deleteChatRoom(String nodeUUID, String roomUUID) throws Exception {
        if (nodeUUID == null) {
            throw new Exception("Node UUID is null");
        } else if (roomUUID == null) {
            throw new Exception("Room UUID is null");
        }
        synchronized (MANAGE_ROOM_MAP) {
            synchronized (CHAT_ROOM_LIST) {
                Iterator<ChatPavilion> iterator = CHAT_ROOM_LIST.iterator();
                while (iterator.hasNext()) {
                    ChatPavilion chatPavilion = iterator.next();
                    if (Objects.equals(chatPavilion.getRoomInfo().getNodeUUID(), nodeUUID)
                            && Objects.equals(chatPavilion.getRoomInfo().getRoomUUID(), roomUUID)) {
                        chatPavilion.disable();
                        iterator.remove();
                        break;
                    }
                }
                MANAGE_ROOM_MAP.remove(roomUUID);
                DaoManager.getRoomDao().deleteRoomInfo(nodeUUID, roomUUID);
            }
        }
        Objects.requireNonNullElse(Cr.CallBack.chatRoomUpdateCallback, () -> {
        }).update();
    }

    /**
     * 从数据库加载全部聊天室
     * 仅在初始化时调用
     */
    static synchronized void loadAllRooms() {
        NodeInfo nodeInfo = Cr.getNodeInfo();
        MemberDao memberDao = DaoManager.getMemberDao();
        if (nodeInfo == null) {
            Logger.warn("Node info is null");
            return;
        }
        String uuid = nodeInfo.getUUID();
        List<RoomInfo> roomInfoList = DaoManager.getRoomDao().getAllRoomInfos();
        synchronized (MANAGE_ROOM_MAP) {
            synchronized (CHAT_ROOM_LIST) {
                for (RoomInfo roomInfo : roomInfoList) {
                    try {
                        ChatPavilion chatRoom = new ChatPavilion(roomInfo);
                        chatRoom.setMemberList(memberDao.getMembers(roomInfo.getNodeUUID(), roomInfo.getRoomUUID()));
                        chatRoom.putMessageList(
                                DaoManager.getMessageDao()
                                        .getMessagesBeforeTime(
                                                roomInfo.getNodeUUID(),
                                                roomInfo.getRoomUUID(),
                                                System.currentTimeMillis(),
                                                Constants.MSG_LIST_BUF_SIZE
                                        )
                        );
                        chatRoom.setMaxMsgID(DaoManager.getMessageDao().getMaxID());
                        CHAT_ROOM_LIST.add(chatRoom);
                        if (Objects.equals(uuid, roomInfo.getNodeUUID())) {
                            MANAGE_ROOM_MAP.put(roomInfo.getRoomUUID(), chatRoom);
                        }
                    } catch (Exception e) {
                        Logger.warn(e);
                    }
                }
            }
        }
        Objects.requireNonNullElse(Cr.CallBack.chatRoomUpdateCallback, () -> {
        }).update();
        Logger.info("Load all rooms");
    }

    /**
     * 卸载全部聊天室
     */
    static synchronized void unloadAllRooms() {
        synchronized (CHAT_ROOM_LIST) {
            Iterator<ChatPavilion> iterator = CHAT_ROOM_LIST.iterator();
            while (iterator.hasNext()) {
                iterator.next().disable();
                iterator.remove();
            }
        }
        synchronized (MANAGE_ROOM_MAP) {
            MANAGE_ROOM_MAP.clear();
        }
        Objects.requireNonNullElse(Cr.CallBack.chatRoomUpdateCallback, () -> {
        }).update();
        Logger.info("Unload all rooms");
    }

    /**
     * 获取全部房间列表
     *
     * @return 房间列表
     */
    static List<ChatRoom> getAllChatRooms() {
        synchronized (CHAT_ROOM_LIST) {
            return Collections.unmodifiableList(CHAT_ROOM_LIST);
        }
    }

    /**
     * 获取被管理的房间列表
     *
     * @return 房间列表
     */
    static List<ChatRoom> getManageRoomList() {
        synchronized (MANAGE_ROOM_MAP) {
            return new ArrayList<>(MANAGE_ROOM_MAP.values());
        }
    }

    /**
     * 根据房间标识码获取被管理的聊天室
     *
     * @param uuid 房间标识码
     * @return 聊天室
     */
    public static ChatRoom getByRoomUUID(String uuid) {
        if (uuid == null) {
            return null;
        }
        synchronized (MANAGE_ROOM_MAP) {
            return MANAGE_ROOM_MAP.get(uuid);
        }
    }

    /**
     * 获取指定节点所管理的房间列表
     *
     * @param nodeUUID 节点标识码
     * @return 房间列表
     */
    static List<ChatRoom> getRoomListByNodeUUID(String nodeUUID) {
        List<ChatRoom> rooms = new ArrayList<>();
        synchronized (CHAT_ROOM_LIST) {
            for (ChatPavilion chatPavilion : CHAT_ROOM_LIST) {
                if (Objects.equals(chatPavilion.getRoomInfo().getNodeUUID(), nodeUUID)) {
                    rooms.add(chatPavilion);
                }
            }
        }
        return rooms;
    }
}
