package icu.mmmc.cr.database;

import icu.mmmc.cr.database.adapters.MemberDaoAdapter;
import icu.mmmc.cr.database.adapters.MessageDaoAdapter;
import icu.mmmc.cr.database.adapters.NodeInfoDaoAdapter;
import icu.mmmc.cr.database.adapters.RoomDaoAdapter;
import icu.mmmc.cr.database.interfaces.MemberDao;
import icu.mmmc.cr.database.interfaces.MessageDao;
import icu.mmmc.cr.database.interfaces.NodeInfoDao;
import icu.mmmc.cr.database.interfaces.RoomDao;

/**
 * 数据访问对象管理器
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class DaoManager {
    private static NodeInfoDao nodeInfoDao;
    private static RoomDao roomDao;
    private static MemberDao memberDao;
    private static MessageDao messageDao;

    public static void setNodeInfoDao(NodeInfoDao nodeInfoDao) {
        DaoManager.nodeInfoDao = nodeInfoDao;
    }

    public static NodeInfoDao getNodeInfoDao() {
        if (nodeInfoDao == null) {
            nodeInfoDao = new NodeInfoDaoAdapter();
        }
        return nodeInfoDao;
    }

    public static void setRoomDao(RoomDao roomDao) {
        DaoManager.roomDao = roomDao;
    }

    public static RoomDao getRoomDao() {
        if (roomDao == null) {
            roomDao = new RoomDaoAdapter();
        }
        return roomDao;
    }

    public static void setMemberDao(MemberDao memberDao) {
        DaoManager.memberDao = memberDao;
    }

    public static MemberDao getMemberDao() {
        if (memberDao == null) {
            memberDao = new MemberDaoAdapter();
        }
        return memberDao;
    }

    public static void setMessageDao(MessageDao messageDao) {
        DaoManager.messageDao = messageDao;
    }

    public static MessageDao getMessageDao() {
        if (messageDao == null) {
            messageDao = new MessageDaoAdapter();
        }
        return messageDao;
    }
}
