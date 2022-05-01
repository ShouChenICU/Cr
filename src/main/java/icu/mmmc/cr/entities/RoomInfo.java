package icu.mmmc.cr.entities;

import icu.mmmc.cr.Serialization;

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
    private String uuid;
    /**
     * 房间名
     */
    private String title;

    public String getNodeUUID() {
        return nodeUUID;
    }

    public RoomInfo setNodeUUID(String nodeUUID) {
        this.nodeUUID = nodeUUID;
        return this;
    }

    public String getUuid() {
        return uuid;
    }

    public RoomInfo setUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public RoomInfo setTitle(String title) {
        this.title = title;
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
        return Objects.equals(nodeUUID, roomInfo.nodeUUID) && Objects.equals(uuid, roomInfo.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeUUID, uuid);
    }

    @Override
    public String toString() {
        return "RoomInfo{" +
                "nodeUUID='" + nodeUUID + '\'' +
                ", uuid='" + uuid + '\'' +
                ", title='" + title + '\'' +
                '}';
    }

    /**
     * 序列化为Bson数据
     *
     * @return 序列化数据
     */
    @Override
    public byte[] serialize() {
        // TODO: 2022/4/26
        return new byte[0];
    }
}
