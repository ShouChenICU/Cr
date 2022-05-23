package icu.mmmc.cr.tasks;

import icu.mmmc.cr.Node;
import icu.mmmc.cr.PacketBody;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.constants.TaskTypes;

/**
 * 同步房间任务（主动
 *
 * @author shouchen
 */
public class SyncRoomTask1 extends AbstractTask {

    public SyncRoomTask1(ProgressCallback callback) {
        super(callback);
    }

    /**
     * 处理包
     *
     * @param packetBody 包
     */
    @Override
    public void handlePacket(PacketBody packetBody) throws Exception {
        super.handlePacket(packetBody);
        done();
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
                .setTaskType(TaskTypes.SYNC_ROOM));
    }
}
