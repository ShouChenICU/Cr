package icu.mmmc.cr.database.interfaces;

import icu.mmmc.cr.entities.NodeInfo;

/**
 * 节点信息数据访问对象
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public interface NodeInfoDao {
    /**
     * 删除节点信息
     *
     * @param uuid 节点唯一标识码
     * @return 是否删除成功
     */
    boolean deleteNodeInfo(String uuid);

    /**
     * 更新或添加节点信息
     *
     * @param nodeInfo 节点信息
     * @return 是否修改成功
     */
    boolean updateNodeInfo(NodeInfo nodeInfo);

    /**
     * 根据uuid查询节点信息
     *
     * @param uuid 节点标识码
     * @return 节点信息
     */
    NodeInfo getByUUID(String uuid);
}
