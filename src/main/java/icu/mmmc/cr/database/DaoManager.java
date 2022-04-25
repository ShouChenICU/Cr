package icu.mmmc.cr.database;

import icu.mmmc.cr.database.interfaces.NodeInfoDao;

/**
 * 数据访问对象管理器
 *
 * @author shouchen
 */
public class DaoManager {
    private static NodeInfoDao nodeInfoDao;

    public static void setNodeInfoDao(NodeInfoDao nodeInfoDao) {
        DaoManager.nodeInfoDao = nodeInfoDao;
    }

    public static NodeInfoDao getNodeInfoDao() {
        return nodeInfoDao;
    }
}
