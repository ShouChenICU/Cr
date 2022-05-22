package icu.mmmc.cr.database.interfaces;

import icu.mmmc.cr.entities.RoomInfo;

import java.util.List;

/**
 * 房间信息访问接口
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public interface RoomDao {
    /**
     * 删除房间
     *
     * @param nodeUUID 所属节点标识码
     * @param roomUUID 房间标识码
     */
    void deleteRoom(String nodeUUID, String roomUUID);

    /**
     * 更新或者新增房间信息
     *
     * @param roomInfo 房间信息
     */
    void updateRoomInfo(RoomInfo roomInfo);

    /**
     * 获取房间信息
     *
     * @param nodeUUID 节点uuid
     * @param roomUUID 房间uuid
     * @return 房间信息
     */
    RoomInfo getByUUID(String nodeUUID, String roomUUID);

    /**
     * 获取全部房间信息
     *
     * @return 全部房间信息列表
     */
    List<RoomInfo> getAll();
}
