package icu.mmmc.cr.tasks;

import icu.mmmc.cr.ChatRoom;
import icu.mmmc.cr.Node;
import icu.mmmc.cr.PacketBody;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.entities.MemberInfo;

import java.util.List;

/**
 * 成员同步任务（主动
 *
 * @author shouchen
 */
public class SyncMemberTask1 extends AbstractTask {
    private final String syncRoomUUID;
    private final List<MemberInfo> memberInfoList;

    public SyncMemberTask1(ChatRoom chatRoom, ProgressCallback callback) {
        super(callback);
        syncRoomUUID = chatRoom.getRoomInfo().getRoomUUID();
        this.memberInfoList = chatRoom.getMemberList();
    }

    /**
     * 处理包
     *
     * @param packetBody 包
     */
    @Override
    public void handlePacket(PacketBody packetBody) throws Exception {
        super.handlePacket(packetBody);
    }

    /**
     * 初始化任务
     *
     * @param node   节点
     * @param taskId 分配的任务id
     */
    @Override
    public void init(Node node, int taskId) {
        super.init(node, taskId);
    }
}
