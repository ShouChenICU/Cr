package icu.mmmc.cr.database.adapters;

import icu.mmmc.cr.database.interfaces.RoomDao;
import icu.mmmc.cr.entities.RoomInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 房间数据访问适配器
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class RoomDaoAdapter implements RoomDao {
    /**
     * 删除房间信息
     *
     * @param nodeUUID 所属节点标识码
     * @param roomUUID 房间标识码
     */
    @Override
    public void deleteRoom(String nodeUUID, String roomUUID) {
    }

    /**
     * 更新或者新增房间信息
     *
     * @param roomInfo 房间信息
     */
    @Override
    public void updateRoomInfo(RoomInfo roomInfo) {
    }

    /**
     * 获取房间信息
     *
     * @param nodeUUID 节点uuid
     * @param roomUUID 房间uuid
     * @return 房间信息
     */
    @Override
    public RoomInfo getByUUID(String nodeUUID, String roomUUID) {
        return null;
    }

    /**
     * 获取全部房间信息
     *
     * @return 全部房间信息列表
     */
    @Override
    public List<RoomInfo> getAll() {
        return new ArrayList<>();
    }

    /**
     * 获取指定房主并且包含指定成员的房间信息列表
     *
     * @param nodeUUID   房主标识码
     * @param memberUUID 成员标识码
     * @return 房间列表
     */
    @Override
    public List<RoomInfo> getByOwnUUIDAndContainMember(String nodeUUID, String memberUUID) {
        return new ArrayList<>();
    }
}
