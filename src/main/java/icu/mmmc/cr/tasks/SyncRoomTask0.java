package icu.mmmc.cr.tasks;

import icu.mmmc.cr.ChatRoom;
import icu.mmmc.cr.ChatRoomManager;
import icu.mmmc.cr.Node;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.entities.RoomInfo;
import icu.mmmc.cr.utils.BsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 同步房间任务（被动
 *
 * @author shouchen
 */
public class SyncRoomTask0 extends AbstractTask {
    private List<RoomInfo> roomInfoList;

    public SyncRoomTask0() {
        super(null);
    }

    /**
     * 处理数据
     *
     * @param data 数据
     */
    @Override
    protected void handleData(byte[] data) throws Exception {
        List<byte[]> list = new ArrayList<>();
        BsonObject object = new BsonObject();
        for (RoomInfo roomInfo : roomInfoList) {
            list.add(roomInfo.serialize());
        }
        object.set("ROOM_LIST", list);
        sendData(TaskTypes.ACK, object.serialize());
        done();
    }

    @Override
    public void init(Node node, int taskId) {
        super.init(node, taskId);
        roomInfoList = new ArrayList<>();
        for (ChatRoom chatRoom : ChatRoomManager.getManageRoomList()) {
            if (chatRoom.containMember(node.getNodeInfo().getUUID())) {
                roomInfoList.add(chatRoom.getRoomInfo());
            }
        }
    }
}
