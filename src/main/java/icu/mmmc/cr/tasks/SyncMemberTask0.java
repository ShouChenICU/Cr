package icu.mmmc.cr.tasks;

import icu.mmmc.cr.ChatRoom;
import icu.mmmc.cr.ChatRoomManager;
import icu.mmmc.cr.PacketBody;
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
     * 处理包
     *
     * @param packetBody 包
     */
    @Override
    public void handlePacket(PacketBody packetBody) throws Exception {
        super.handlePacket(packetBody);
        if (stepCount == 0) {
            String roomUUID = new String(packetBody.getPayload(), StandardCharsets.UTF_8);
            ChatRoom chatRoom = ChatRoomManager.getByRoomUUID(roomUUID);
            if (chatRoom == null) {
                String err = "Room not found";
                node.postPacket(new PacketBody()
                        .setSource(taskId)
                        .setDestination(packetBody.getSource())
                        .setTaskType(TaskTypes.ERROR)
                        .setPayload(err.getBytes(StandardCharsets.UTF_8)));
                halt(err);
                return;
            }
            memberInfoList = chatRoom.getMemberList();
            stepCount = 1;
            node.postPacket(new PacketBody()
                    .setSource(taskId)
                    .setDestination(packetBody.getSource())
                    .setTaskType(TaskTypes.ACK));
        } else if (stepCount == 1) {

        }
    }
}
