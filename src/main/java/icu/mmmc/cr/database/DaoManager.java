package icu.mmmc.cr.database;

import icu.mmmc.cr.database.interfaces.NodeInfoDao;
import icu.mmmc.cr.database.interfaces.RoomInfoDao;

/**
 * 数据访问对象管理器
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class DaoManager {
    private static NodeInfoDao nodeInfoDao;
    private static RoomInfoDao roomInfoDao;

    public static void setNodeInfoDao(NodeInfoDao nodeInfoDao) {
        DaoManager.nodeInfoDao = nodeInfoDao;
    }

    public static NodeInfoDao getNodeInfoDao() {
        return nodeInfoDao;
    }

    public static void setRoomInfoDao(RoomInfoDao roomInfoDao) {
        DaoManager.roomInfoDao = roomInfoDao;
    }

    public static RoomInfoDao getRoomInfoDao() {
        return roomInfoDao;
    }
}
