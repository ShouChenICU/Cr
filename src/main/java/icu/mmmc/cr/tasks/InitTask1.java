package icu.mmmc.cr.tasks;

import icu.mmmc.cr.Node;
import icu.mmmc.cr.PacketBody;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.constants.TaskTypes;

/**
 * 初始化任务 主动
 *
 * @author shouchen
 */
public class InitTask1 extends AbstractTask {
    private int idCount;

    public InitTask1(ProgressCallback callback) {
        super(callback);
        idCount = 0;
    }

    /**
     * 处理包
     *
     * @param packetBody 包
     */
    @Override
    public void handlePacket(PacketBody packetBody) {

    }

    @Override
    public void init(Node node, int taskId) {
        super.init(node, taskId);
        node.postPacket(new PacketBody()
                .setSource(taskId)
                .setDestination(0)
                .setTaskType(TaskTypes.INIT));
    }
}
