package icu.mmmc.cr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 聊天室管理器
 *
 * @author shouchen
 */
class CharRoomManager {
    private static final List<ChatRoom> CHAT_ROOM_LIST;

    static {
        CHAT_ROOM_LIST = new ArrayList<>();
    }

    /**
     * 添加聊天室
     *
     * @param chatRoom 聊天室
     */
    public static void addChatRoom(ChatRoom chatRoom) {
        synchronized (CHAT_ROOM_LIST) {
            CHAT_ROOM_LIST.add(chatRoom);
        }
    }

    /**
     * 加载全部聊天室
     */
    public static void loadAll() {

    }

    /**
     * 获取聊天室列表
     *
     * @return 聊天室列表
     */
    public static List<ChatRoom> getChatRoomList() {
        synchronized (CHAT_ROOM_LIST) {
            return Collections.unmodifiableList(CHAT_ROOM_LIST);
        }
    }
}
