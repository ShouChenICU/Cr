package icu.mmmc.cr.entities;

import icu.mmmc.cr.exceptions.EntityBrokenException;
import icu.mmmc.cr.utils.BsonObject;
import icu.mmmc.cr.utils.BsonUtils;
import icu.mmmc.cr.utils.KeyUtils;
import icu.mmmc.cr.utils.SignUtils;
import org.bson.BSONObject;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 节点信息
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class NodeInfo implements Serialization, Checkable {
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
    /**
     * 时间戳
     */
    private long timestamp;
    /**
     * 签名
     */
    private byte[] signature;

    public NodeInfo() {
        attributes = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    public NodeInfo(byte[] dat) throws Exception {
        BSONObject object = BsonUtils.deserialize(dat);
        uuid = (String) object.get("UUID");
        publicKey = KeyUtils.getPubKeyByCode((byte[]) object.get("PUB_KEY"));
        attributes = (Map<String, String>) object.get("ATTRIBUTES");
        timestamp = (long) object.get("TIMESTAMP");
        signature = (byte[]) object.get("SIGNATURE");
        check();
    }

    /**
     * 用私钥签名节点的基本信息
     * 签名的数据包括属性集和时间戳
     *
     * @param privateKey 私钥
     * @throws Exception 签名异常
     */
    public NodeInfo sign(PrivateKey privateKey) throws Exception {
        synchronized (this) {
            long t = System.currentTimeMillis();
            signature = SignUtils.sign(
                    privateKey,
                    new BsonObject()
                            .set("ATTRIBUTES", attributes)
                            .set("TIMESTAMP", t)
                            .serialize()
            );
            timestamp = t;
            return this;
        }
    }

    /**
     * 验证节点信息
     * 1、验证基本信息是否完整
     * 2、验证签名是否有效
     *
     * @throws Exception 验证不通过
     */
    @Override
    public void check() throws Exception {
        if (uuid == null
                || publicKey == null
                || !Objects.equals(uuid, UUID.nameUUIDFromBytes(publicKey.getEncoded()).toString())
                || attributes == null) {
            throw new EntityBrokenException("Node info broken");
        } else if (signature == null) {
            throw new EntityBrokenException("Signature is empty");
        } else if (
                !SignUtils.verify(
                        publicKey,
                        new BsonObject()
                                .set("ATTRIBUTES", attributes)
                                .set("TIMESTAMP", timestamp)
                                .serialize(),
                        signature
                )
        ) {
            throw new EntityBrokenException("Signature verification failed");
        } else if (timestamp > System.currentTimeMillis()) {
            throw new EntityBrokenException("Timestamp is beyond current time");
        }
    }

    public NodeInfo setUUID(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public String getUUID() {
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

    public long getTimestamp() {
        return timestamp;
    }

    public NodeInfo setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public byte[] getSignature() {
        return signature;
    }

    public NodeInfo setSignature(byte[] signature) {
        this.signature = signature;
        return this;
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

    @Override
    public String toString() {
        return "NodeInfo{" +
                "uuid='" + uuid + '\'' +
                ", publicKey=" + (publicKey == null ? "null" : "***") +
                ", attributes=" + attributes +
                ", timestamp=" + timestamp +
                ", signature=" + (signature == null ? "null" : "***") +
                '}';
    }

    /**
     * 序列化为Bson数据
     *
     * @return 序列化数据
     */
    @Override
    public byte[] serialize() {
        return new BsonObject()
                .set("UUID", uuid)
                .set("PUB_KEY", publicKey.getEncoded())
                .set("ATTRIBUTES", attributes)
                .set("TIMESTAMP", timestamp)
                .set("SIGNATURE", signature)
                .serialize();
    }
}
