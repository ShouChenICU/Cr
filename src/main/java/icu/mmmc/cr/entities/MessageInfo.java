package icu.mmmc.cr.entities;

import icu.mmmc.cr.exceptions.EntityBrokenException;
import icu.mmmc.cr.utils.BsonObject;
import icu.mmmc.cr.utils.BsonUtils;
import org.bson.BSONObject;

import java.util.Objects;

/**
 * 消息信息
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class MessageInfo implements Serialization, Checkable {
    /**
     * 节点标识码
     */
    private String nodeUUID;
    /**
     * 房间标识码
     */
    private String roomUUID;
    /**
     * 消息id
     */
    private int id;
    /**
     * 发送者标识码
     */
    private String senderUUID;
    /**
     * 消息类型
     */
    private int type;
    /**
     * 消息内容
     */
    private String content;
    /**
     * 时间戳
     */
    private long timestamp;

    public MessageInfo() {
    }

    public MessageInfo(byte[] dat) throws Exception {
        BSONObject object = BsonUtils.deserialize(dat);
        nodeUUID = (String) object.get("NODE_UUID");
        roomUUID = (String) object.get("ROOM_UUID");
        id = (int) object.get("ID");
        senderUUID = (String) object.get("SENDER");
        type = (int) object.get("TYPE");
        content = (String) object.get("CONTENT");
        timestamp = (long) object.get("TIMESTAMP");
        check();
    }

    @Override
    public void check() throws EntityBrokenException {
        if (nodeUUID == null || roomUUID == null) {
            throw new EntityBrokenException("Message info broken");
        }
    }

    public String getNodeUUID() {
        return nodeUUID;
    }

    public MessageInfo setNodeUUID(String nodeUUID) {
        this.nodeUUID = nodeUUID;
        return this;
    }

    public String getRoomUUID() {
        return roomUUID;
    }

    public MessageInfo setRoomUUID(String roomUUID) {
        this.roomUUID = roomUUID;
        return this;
    }

    public int getId() {
        return id;
    }

    public MessageInfo setId(int id) {
        this.id = id;
        return this;
    }

    public String getSenderUUID() {
        return senderUUID;
    }

    public MessageInfo setSenderUUID(String senderUUID) {
        this.senderUUID = senderUUID;
        return this;
    }

    public int getType() {
        return type;
    }

    public MessageInfo setType(int type) {
        this.type = type;
        return this;
    }

    public String getContent() {
        return content;
    }

    public MessageInfo setContent(String content) {
        this.content = content;
        return this;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public MessageInfo setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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
        MessageInfo that = (MessageInfo) o;
        return id == that.id && Objects.equals(nodeUUID, that.nodeUUID) && Objects.equals(roomUUID, that.roomUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeUUID, roomUUID, id);
    }

    @Override
    public String toString() {
        return "MessageInfo{" +
                "nodeUUID='" + nodeUUID + '\'' +
                ", roomUUID='" + roomUUID + '\'' +
                ", id=" + id +
                ", senderUUID='" + senderUUID + '\'' +
                ", type=" + type +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
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
                .set("NODE_UUID", nodeUUID)
                .set("ROOM_UUID", roomUUID)
                .set("ID", id)
                .set("SENDER", senderUUID)
                .set("TYPE", type)
                .set("CONTENT", content)
                .set("TIMESTAMP", timestamp)
                .serialize();
    }
}
