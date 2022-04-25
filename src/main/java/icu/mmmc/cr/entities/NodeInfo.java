package icu.mmmc.cr.entities;

import icu.mmmc.cr.Serialization;
import icu.mmmc.cr.utils.BsonUtils;
import icu.mmmc.cr.utils.KeyUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 节点信息
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class NodeInfo implements Serialization {
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
     * 节点属性集
     */
    private final Map<String, String> attributes;

    public NodeInfo() {
        attributes = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public NodeInfo(byte[] dat) {
        BSONObject object = BsonUtils.deserialize(dat);
        uuid = (String) object.get("UUID");
        publicKey = KeyUtils.getPubKeyByCode((byte[]) object.get("PUB_KEY"));
        attributes = (Map<String, String>) object.get("ATTRIBUTES");
    }

    public NodeInfo setUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public String getUuid() {
        return uuid;
    }

    public NodeInfo setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public NodeInfo setAttr(String key, String value) {
        attributes.put(key, value);
        return this;
    }

    public String getAttr(String key) {
        return attributes.get(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NodeInfo nodeInfo = (NodeInfo) o;
        return Objects.equals(uuid, nodeInfo.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    /**
     * 序列化为Bson数据
     *
     * @return 序列化数据
     */
    @Override
    public byte[] serialize() {
        BSONObject object = new BasicBSONObject();
        object.put("UUID", uuid);
        object.put("PUB_KEY", publicKey.getEncoded());
        object.put("ATTRIBUTES", attributes);
        return BsonUtils.serialize(object);
    }
}
