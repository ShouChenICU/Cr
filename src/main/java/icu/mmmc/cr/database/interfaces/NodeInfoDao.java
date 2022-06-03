package icu.mmmc.cr.database.interfaces;

import icu.mmmc.cr.entities.NodeInfo;

import java.util.List;

/**
 * 节点信息数据访问接口
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public interface NodeInfoDao {
    /**
     * 删除节点信息
     *
     * @param uuid 节点唯一标识码
     */
    void deleteNodeInfo(String uuid);

    /**
     * 更新或添加节点信息
     *
     * @param nodeInfo 节点信息
     */
    void updateNodeInfo(NodeInfo nodeInfo);

    /**
     * 根据uuid查询节点信息
     *
     * @param uuid 节点标识码
     * @return 节点信息
     */
    NodeInfo getByUUID(String uuid);

    /**
     * 获取全部已保存的节点信息
     *
     * @return 节点信息列表
     */
    List<NodeInfo> getAllNodeInfos();
}
