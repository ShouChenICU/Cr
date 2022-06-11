package icu.mmmc.cr.callbacks;

import icu.mmmc.cr.entities.NodeInfo;

/**
 * 节点列表或者节点信息更新回调
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public interface NodeStatusUpdateCallback {
    /**
     * 新的节点连接
     *
     * @param nodeInfo 节点信息
     */
    void connected(NodeInfo nodeInfo);

    /**
     * 节点断开连接
     *
     * @param nodeInfo 节点信息
     * @param reason   原因
     */
    void disconnected(NodeInfo nodeInfo, String reason);
}
