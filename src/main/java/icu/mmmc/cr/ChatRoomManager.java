package icu.mmmc.cr;

import icu.mmmc.cr.database.DaoManager;
import icu.mmmc.cr.database.interfaces.RoomInfoDao;
import icu.mmmc.cr.entities.RoomInfo;
import icu.mmmc.cr.utils.Logger;

import java.util.*;

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
    private static final List<ChatRoom> CHAT_ROOM_LIST;
    /**
     * 被管理的聊天室map(房主是自己)
     */
    private static final Map<String, ChatRoom> MANAGE_ROOM_MAP;

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
        if (node == null || node.getNodeInfo() == null || node.getNodeInfo().getUuid() == null) {
            Logger.warn("node info broken");
            return;
        }
        String uuid = node.getNodeInfo().getUuid();
        Logger.debug("register node " + uuid);
        synchronized (MANAGE_ROOM_MAP) {
            for (ChatRoom chatRoom : MANAGE_ROOM_MAP.values()) {
                if (chatRoom.containsMember(uuid)) {
                    chatRoom.putNode(uuid, node);
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
            Logger.warn("uuid is null");
            return;
        }
        Logger.debug("unregister node " + uuid);
        synchronized (MANAGE_ROOM_MAP) {
            for (ChatRoom chatRoom : MANAGE_ROOM_MAP.values()) {
                chatRoom.removeNode(uuid);
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
            RoomInfoDao dao = DaoManager.getRoomInfoDao();
            if (dao != null) {
                dao.updateRoomInfo(roomInfo);
            }
            synchronized (CHAT_ROOM_LIST) {
                for (ChatRoom chatRoom : CHAT_ROOM_LIST) {
                    if (Objects.equals(roomInfo, chatRoom.getRoomInfo())) {
                        chatRoom.updateRoomInfo(roomInfo);
                        Logger.debug("update room info. node:" + roomInfo.getNodeUUID() + " room:" + roomInfo.getRoomUUID());
                        Objects.requireNonNullElse(Cr.CallBack.chatRoomUpdateCallback, () -> {
                        }).update();
                        return;
                    }
                }
                ChatRoom chatRoom = new ChatRoom(roomInfo);
                CHAT_ROOM_LIST.add(chatRoom);
                Logger.debug("add a new room info. node:" + roomInfo.getNodeUUID() + " room:" + roomInfo.getRoomUUID());
                Objects.requireNonNullElse(Cr.CallBack.chatRoomUpdateCallback, () -> {
                }).update();
            }
        } catch (Exception e) {
            Logger.warn(e);
        }
    }

    /**
     * 创建聊天室
     */
    static ChatRoom createChatRoom() {
        // TODO: 2022/5/7
        Objects.requireNonNullElse(Cr.CallBack.chatRoomUpdateCallback, () -> {
        }).update();
        return null;
    }

    /**
     * 从数据库加载全部聊天室
     * 仅在初始化时调用
     */
    static synchronized void loadAll() {
        RoomInfoDao dao = DaoManager.getRoomInfoDao();
        if (dao == null) {
            return;
        }
        List<RoomInfo> roomInfoList = dao.getAll();
        // TODO: 2022/5/19
        Objects.requireNonNullElse(Cr.CallBack.chatRoomUpdateCallback, () -> {
        }).update();
    }

    /**
     * 卸载全部聊天室
     */
    static synchronized void unloadAll() {
        synchronized (CHAT_ROOM_LIST) {
            CHAT_ROOM_LIST.clear();
        }
        synchronized (MANAGE_ROOM_MAP) {
            MANAGE_ROOM_MAP.clear();
        }
        Objects.requireNonNullElse(Cr.CallBack.chatRoomUpdateCallback, () -> {
        }).update();
    }

    /**
     * 获取聊天室列表
     *
     * @return 聊天室列表
     */
    static List<ChatRoom> getChatRoomList() {
        synchronized (CHAT_ROOM_LIST) {
            return Collections.unmodifiableList(CHAT_ROOM_LIST);
        }
    }
}
