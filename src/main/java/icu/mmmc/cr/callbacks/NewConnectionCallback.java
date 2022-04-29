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
     * @param name  如果是未知节点，则为uuid,否则为节点名称
     * @param isNew 当新连接为未知节点时为true,否则false
     * @return 返回true允许连接，否则拒绝连接
     */
    boolean newConnection(String name, boolean isNew);
}
