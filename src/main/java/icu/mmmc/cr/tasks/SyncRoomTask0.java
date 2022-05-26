package icu.mmmc.cr.tasks;

import icu.mmmc.cr.Cr;
import icu.mmmc.cr.Node;
import icu.mmmc.cr.PacketBody;
import icu.mmmc.cr.WorkerThreadPool;
import icu.mmmc.cr.callbacks.adapters.ProgressAdapter;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.database.DaoManager;
import icu.mmmc.cr.entities.RoomInfo;
import icu.mmmc.cr.utils.Logger;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 同步房间任务（被动
 *
 * @author shouchen
 */
public class SyncRoomTask0 extends AbstractTask {
    private CountDownLatch latch;
    private List<RoomInfo> roomInfoList;

    public SyncRoomTask0() {
        super(null);
    }

    /**
     * 处理包
     *
     * @param packetBody 包
     */
    @Override
    public void handlePacket(PacketBody packetBody) throws Exception {
        super.handlePacket(packetBody);
        while (!roomInfoList.isEmpty()) {
            RoomInfo roomInfo = roomInfoList.remove(0);
            node.addTask(new PushTask(roomInfo, new ProgressAdapter() {
                @Override
                public void done() {
                    latch.countDown();
                }

                @Override
                public void halt(String msg) {
                    latch.countDown();
                }
            }));
        }
        WorkerThreadPool.execute(() -> {
            try {
                latch.await();
                node.postPacket(new PacketBody()
                        .setSource(taskId)
                        .setDestination(packetBody.getSource())
                        .setTaskType(TaskTypes.ACK));
                done();
            } catch (InterruptedException e) {
                Logger.warn(e);
                halt(e.toString());
            }
        });
    }

    @Override
    public void init(Node node, int taskId) {
        super.init(node, taskId);
        roomInfoList = DaoManager.getRoomDao().getByOwnUUIDAndContainMember(Cr.getNodeInfo().getUuid(), node.getNodeInfo().getUuid());
        latch = new CountDownLatch(roomInfoList.size());
    }
}
