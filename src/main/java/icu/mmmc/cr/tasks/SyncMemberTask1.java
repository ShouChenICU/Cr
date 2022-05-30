package icu.mmmc.cr.tasks;

import icu.mmmc.cr.ChatRoom;
import icu.mmmc.cr.Node;
import icu.mmmc.cr.PacketBody;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.entities.MemberInfo;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 成员同步任务（主动
 *
 * @author shouchen
 */
public class SyncMemberTask1 extends AbstractTask {
    private final String syncRoomUUID;
    private final List<MemberInfo> memberInfoList;
    private int stepCount;

    public SyncMemberTask1(ChatRoom chatRoom, ProgressCallback callback) {
        super(callback);
        stepCount = 0;
        syncRoomUUID = chatRoom.getRoomInfo().getRoomUUID();
        this.memberInfoList = chatRoom.getMemberList();
    }

    /**
     * 处理数据
     *
     * @param data 数据
     */
    @Override
    protected void handleData(byte[] data) throws Exception {
        if (stepCount == 0) {

        }
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
        node.postPacket(new PacketBody()
                .setSource(taskId)
                .setDestination(0)
                .setTaskType(TaskTypes.SYNC_MEMBER)
                .setPayload(syncRoomUUID.getBytes(StandardCharsets.UTF_8)));
    }
}
