package icu.mmmc.cr.callbacks;

import icu.mmmc.cr.entities.RoomInfo;

/**
 * 请求加入新的房间
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public interface JoinNewRoomCallback {
    /**
     * 是否允许加入新的房间
     *
     * @param roomInfo 房间信息
     * @return 加入则返回true, 否则返回false
     */
    boolean joinNewRoom(RoomInfo roomInfo);
}
