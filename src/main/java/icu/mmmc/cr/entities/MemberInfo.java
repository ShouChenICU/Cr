package icu.mmmc.cr.entities;

import icu.mmmc.cr.exceptions.EntityBrokenException;
import icu.mmmc.cr.utils.BsonObject;
import icu.mmmc.cr.utils.BsonUtils;
import org.bson.BSONObject;

import java.util.Objects;

/**
 * 房间成员信息
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class MemberInfo implements Serialization, Checkable {
    /**
     * 所属节点标识码
     */
    private String nodeUUID;
    /**
     * 房间标识码
     */
    private String roomUUID;
    /**
     * 用户标识码
     */
    private String userUUID;
    /**
     * 更新时间
     */
    private long updateTime;

    public MemberInfo() {
    }

    public MemberInfo(byte[] dat) throws Exception {
        BSONObject object = BsonUtils.deserialize(dat);
        nodeUUID = (String) object.get("NODE_UUID");
        roomUUID = (String) object.get("ROOM_UUID");
        userUUID = (String) object.get("USER_UUID");
        updateTime = (long) object.get("UPDATE_TIME");
        check();
    }

    @Override
    public void check() throws EntityBrokenException {
        if (nodeUUID == null || roomUUID == null || userUUID == null) {
            throw new EntityBrokenException("Member info broken");
        }
    }

    public String getNodeUUID() {
        return nodeUUID;
    }

    public MemberInfo setNodeUUID(String nodeUUID) {
        this.nodeUUID = nodeUUID;
        return this;
    }

    public String getRoomUUID() {
        return roomUUID;
    }

    public MemberInfo setRoomUUID(String roomUUID) {
        this.roomUUID = roomUUID;
        return this;
    }

    public String getUserUUID() {
        return userUUID;
    }

    public MemberInfo setUserUUID(String userUUID) {
        this.userUUID = userUUID;
        return this;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public MemberInfo setUpdateTime(long updateTime) {
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
        MemberInfo that = (MemberInfo) o;
        return Objects.equals(nodeUUID, that.nodeUUID) && Objects.equals(roomUUID, that.roomUUID) && Objects.equals(userUUID, that.userUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeUUID, roomUUID, userUUID);
    }

    @Override
    public String toString() {
        return "MemberInfo{" +
                "nodeUUID='" + nodeUUID + '\'' +
                ", roomUUID='" + roomUUID + '\'' +
                ", userUUID='" + userUUID + '\'' +
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
                .set("USER_UUID", userUUID)
                .set("UPDATE_TIME", updateTime)
                .serialize();
    }
}
