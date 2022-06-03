package icu.mmmc.cr.callbacks;

/**
 * 新连接回调
 *
 * @author shouchen
 */
public interface NewConnectionCallback {
    /**
     * 新连接接入
     *
     * @param uuid  节点标识码
     * @param title 节点名称
     * @param isNew 当新连接为未知节点时为true,否则false
     * @return 返回true允许连接，否则拒绝连接
     */
    boolean newConnection(String uuid, String title, boolean isNew);
}
