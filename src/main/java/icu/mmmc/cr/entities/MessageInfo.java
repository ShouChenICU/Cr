package icu.mmmc.cr.entities;

import icu.mmmc.cr.Serialization;
import icu.mmmc.cr.utils.BsonUtils;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;

/**
 * 消息信息
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class MessageInfo implements Serialization {
    private String nodeUUID;
    private String roomUUID;
    private int id;
    private String senderUUID;
    private int type;
    private String content;
    private long timeStamp;

    public MessageInfo() {
    }

    public MessageInfo(byte[] dat) {
        BSONObject object = BsonUtils.deserialize(dat);
        nodeUUID = (String) object.get("NODE_UUID");
        roomUUID = (String) object.get("ROOM_UUID");
        id = (int) object.get("ID");
        senderUUID = (String) object.get("SENDER");
        type = (int) object.get("TYPE");
        content = (String) object.get("CONTENT");
        timeStamp = (long) object.get("TIME_STAMP");
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

    public long getTimeStamp() {
        return timeStamp;
    }

    public MessageInfo setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
        return this;
    }

    /**
     * 序列化为Bson数据
     *
     * @return 序列化数据
     */
    @Override
    public byte[] serialize() {
        BSONObject object = new BasicBSONObject();
        object.put("NODE_UUID", nodeUUID);
        object.put("ROOM_UUID", roomUUID);
        object.put("ID", id);
        object.put("SENDER", senderUUID);
        object.put("TYPE", type);
        object.put("CONTENT", content);
        object.put("TIME_STAMP", timeStamp);
        return BsonUtils.serialize(object);
    }
}
