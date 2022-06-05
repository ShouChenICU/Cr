package icu.mmmc.cr.utils;

import icu.mmmc.cr.constants.NodeAttributes;
import icu.mmmc.cr.entities.NodeInfo;

import java.security.KeyPair;
import java.util.UUID;

/**
 * 节点工厂
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public final class NodeFactory {
    /**
     * 生成一个节点信息
     *
     * @return 节点信息
     */
    public static NodeInfo genNodeInfo(String title, KeyPair keyPair) throws Exception {
        return new NodeInfo()
                .setUUID(UUID.nameUUIDFromBytes(keyPair.getPublic().getEncoded()).toString())
                .setPublicKey(keyPair.getPublic())
                .setAttr(NodeAttributes.$TITLE, title)
                .sign(keyPair.getPrivate());
    }
}
