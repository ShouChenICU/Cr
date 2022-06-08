package icu.mmmc.cr.tasks;

import icu.mmmc.cr.ChatRoom;
import icu.mmmc.cr.ChatRoomManager;
import icu.mmmc.cr.Node;
import icu.mmmc.cr.WorkerThreadPool;
import icu.mmmc.cr.callbacks.adapters.ProgressAdapter;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.entities.RoomInfo;
import icu.mmmc.cr.utils.Logger;

import java.util.ArrayList;
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
     * 处理数据
     *
     * @param data 数据
     */
    @Override
    protected void handleData(byte[] data) throws Exception {
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
                sendData(TaskTypes.ACK, null);
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
        roomInfoList = new ArrayList<>();
        for (ChatRoom chatRoom : ChatRoomManager.getManageRoomList()) {
            if (chatRoom.containMember(node.getNodeInfo().getUUID())) {
                roomInfoList.add(chatRoom.getRoomInfo());
            }
        }
        latch = new CountDownLatch(roomInfoList.size());
    }
}
