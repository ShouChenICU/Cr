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

    @Override
    protected void dataHandler(byte[] data) throws Exception {

    }

    @Override
    protected void exceptionHandler(Exception e) {
        Logger.error(e);

    }
}
