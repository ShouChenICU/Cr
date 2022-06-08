package icu.mmmc.cr.tasks;

import icu.mmmc.cr.ChatRoomManager;
import icu.mmmc.cr.Node;
import icu.mmmc.cr.PacketBody;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.entities.RoomInfo;
import icu.mmmc.cr.utils.BsonUtils;
import org.bson.BSONObject;

import java.util.List;

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
     * 处理数据
     *
     * @param data 数据
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void handleData(byte[] data) {
        BSONObject object = BsonUtils.deserialize(data);
        List<byte[]> list = (List<byte[]>) object.get("ROOM_LIST");
        for (byte[] dat : list) {
            try {
                RoomInfo roomInfo = new RoomInfo(dat);
                ChatRoomManager.updateRoomInfo(roomInfo);
            } catch (Exception e) {
                halt(e.toString());
                return;
            }
        }
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
