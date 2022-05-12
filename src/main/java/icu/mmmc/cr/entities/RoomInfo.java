package icu.mmmc.cr.entities;

import icu.mmmc.cr.Serialization;
import icu.mmmc.cr.exceptions.EntityBrokenException;
import icu.mmmc.cr.utils.BsonObject;
import icu.mmmc.cr.utils.BsonUtils;
import org.bson.BSONObject;

import java.util.Objects;

/**
 * 房间信息
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class RoomInfo implements Serialization {
    /**
     * 所属节点的标识码
     */
    private String nodeUUID;
    /**
     * 房间标识码
     */
    private String roomUUID;
    /**
     * 房间名
     */
    private String title;
    /**
     * 更新时间
     */
    private long updateTime;

    public RoomInfo() {
    }

    public RoomInfo(byte[] dat) throws Exception {
        BSONObject object = BsonUtils.deserialize(dat);
        nodeUUID = (String) object.get("NODE_UUID");
        roomUUID = (String) object.get("ROOM_UUID");
        title = (String) object.get("TITLE");
        updateTime = (long) object.get("UPDATE_TIME");
        check();
    }

    public void check() throws EntityBrokenException {
        if (nodeUUID == null || roomUUID == null | title == null) {
            throw new EntityBrokenException("Room info broken");
        }
    }

    public String getNodeUUID() {
        return nodeUUID;
    }

    public RoomInfo setNodeUUID(String nodeUUID) {
        this.nodeUUID = nodeUUID;
        return this;
    }

    public String getRoomUUID() {
        return roomUUID;
    }

    public RoomInfo setRoomUUID(String roomUUID) {
        this.roomUUID = roomUUID;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public RoomInfo setTitle(String title) {
        this.title = title;
        return this;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public RoomInfo setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
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
        RoomInfo roomInfo = (RoomInfo) o;
        return Objects.equals(nodeUUID, roomInfo.nodeUUID) && Objects.equals(roomUUID, roomInfo.roomUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeUUID, roomUUID);
    }

    @Override
    public String toString() {
        return "RoomInfo{" +
                "nodeUUID='" + nodeUUID + '\'' +
                ", roomUUID='" + roomUUID + '\'' +
                ", title='" + title + '\'' +
                ", updateTime=" + updateTime +
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
                .set("TITLE", title)
                .set("UPDATE_TIME", updateTime)
                .serialize();
    }
}
