package icu.mmmc.cr.entities;

import icu.mmmc.cr.Serialization;

/**
 * 房间信息
 *
 * @author shouchen
 */
public class RoomInfo implements Serialization {
    /**
     * 所属节点的标识码
     */
    private String nodeUUID;
    /**
     * 房间id
     */
    private String id;
    /**
     * 房间名
     */
    private String title;

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
