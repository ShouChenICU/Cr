package icu.mmmc.cr.entities;

import java.security.PublicKey;
import java.util.Map;

/**
 * 节点信息
 *
 * @author shouchen
 */
public class NodeInfo {
    /**
     * 全球唯一标识符
     * 使用UUID第3版以公钥为命名空间生成
     */
    private String uuid;
    /**
     * RSA公钥
     */
    private PublicKey publicKey;
    /**
     * 网络地址
     */
    private String host;
    /**
     * 连接端口
     */
    private int port;
    /**
     * 节点属性集
     */
    private Map<String, String> attributes;
}
