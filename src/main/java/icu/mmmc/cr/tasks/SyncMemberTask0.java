package icu.mmmc.cr.tasks;

import icu.mmmc.cr.ChatRoom;
import icu.mmmc.cr.ChatRoomManager;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.entities.MemberInfo;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 成员同步任务（被动
 *
 * @author shouchen
 */
public class SyncMemberTask0 extends AbstractTask {
    private List<MemberInfo> memberInfoList;
    private int stepCount;

    public SyncMemberTask0() {
        super(null);
        stepCount = 0;
    }

    /**
     * 处理数据
     *
     * @param data 数据
     */
    @Override
    protected void handleData(byte[] data) throws Exception {
        if (stepCount == 0) {
            String roomUUID = new String(data, StandardCharsets.UTF_8);
            ChatRoom chatRoom = ChatRoomManager.getByRoomUUID(roomUUID);
            if (chatRoom == null) {
                String err = "Room not found";
                sendError(err);
                halt(err);
                return;
            }
            memberInfoList = chatRoom.getMemberList();
            stepCount = 1;
            sendData(TaskTypes.ACK, null);
        } else if (stepCount == 1) {

        }
    }
}
