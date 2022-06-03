package icu.mmmc.cr.database.adapters;

import icu.mmmc.cr.database.interfaces.NodeInfoDao;
import icu.mmmc.cr.entities.NodeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 节点信息访问适配器
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class NodeInfoDaoAdapter implements NodeInfoDao {
    /**
     * 删除节点信息
     *
     * @param uuid 节点唯一标识码
     */
    @Override
    public void deleteNodeInfo(String uuid) {
    }

    /**
     * 更新或添加节点信息
     *
     * @param nodeInfo 节点信息
     */
    @Override
    public void updateNodeInfo(NodeInfo nodeInfo) {
    }

    /**
     * 根据uuid查询节点信息
     *
     * @param uuid 节点标识码
     * @return 节点信息
     */
    @Override
    public NodeInfo getByUUID(String uuid) {
        return null;
    }

    /**
     * 获取全部已保存的节点信息
     *
     * @return 节点信息列表
     */
    @Override
    public List<NodeInfo> getAllNodeInfos() {
        return new ArrayList<>();
    }
}
