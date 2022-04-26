package icu.mmmc.cr;

import icu.mmmc.cr.entities.NodeInfo;
import icu.mmmc.cr.utils.Logger;

import java.nio.channels.SelectionKey;

/**
 * 节点
 * 一个节点就代表一个Cr实例
 *
 * @author shouchen
 */
abstract class Node extends NetNode {
    private NodeInfo nodeInfo;

    public Node(SelectionKey key) {
        super(key);
    }

    /**
     * 数据包处理
     *
     * @param data 数据
     */
    @Override
    protected void dataHandler(byte[] data) throws Exception {
        // TODO: 2022/4/26
    }

    @Override
    public void disconnect() throws Exception {
        super.disconnect();
        // TODO: 2022/4/26
    }

    /**
     * 异常处理
     *
     * @param e 异常
     */
    @Override
    protected void exceptionHandler(Exception e) {
        Logger.error(e);
        try {
            disconnect();
        } catch (Exception ex) {
            Logger.warn(ex);
        }
    }

    /**
     * 初始化完成
     */
    abstract void initDone();
}
