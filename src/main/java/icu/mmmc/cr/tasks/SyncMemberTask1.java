package icu.mmmc.cr.tasks;

import icu.mmmc.cr.ChatPavilion;
import icu.mmmc.cr.ChatRoom;
import icu.mmmc.cr.Node;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.entities.MemberInfo;
import icu.mmmc.cr.utils.BsonObject;
import icu.mmmc.cr.utils.BsonUtils;
import icu.mmmc.cr.utils.Logger;
import org.bson.BasicBSONObject;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 成员同步任务（主动
 *
 * @author shouchen
 */
public class SyncMemberTask1 extends AbstractTask {
    private final ChatPavilion chatPavilion;
    private final String syncRoomUUID;
    private final List<MemberInfo> memberInfoList;
    private int stepCount;

    public SyncMemberTask1(ChatRoom chatRoom, ProgressCallback callback) {
        super(callback);
        chatPavilion = (ChatPavilion) chatRoom;
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
            BsonObject obj = new BsonObject();
            for (MemberInfo info : memberInfoList) {
                obj.set(info.getUserUUID(), info.getUpdateTime());
            }
            stepCount = 1;
            sendData(TaskTypes.ACK, obj.serialize());
        } else if (stepCount == 1) {
            stepCount = 2;
            BasicBSONObject object = (BasicBSONObject) BsonUtils.deserialize(data);
            for (String uuid : object.keySet()) {
                try {
                    chatPavilion.deleteMember(uuid);
                } catch (Exception e) {
                    Logger.warn(e);
                }
            }
            sendData(TaskTypes.ACK, null);
        } else if (stepCount == 2) {
            stepCount = 3;
            BasicBSONObject update = (BasicBSONObject) BsonUtils.deserialize(data);
            for (Object dat : update.values()) {
                MemberInfo info = new MemberInfo((byte[]) dat);
                chatPavilion.updateMemberInfo(info);
            }
            sendData(TaskTypes.ACK, null);
        } else if (stepCount == 3) {
            BasicBSONObject update = (BasicBSONObject) BsonUtils.deserialize(data);
            for (Object dat : update.values()) {
                MemberInfo info = new MemberInfo((byte[]) dat);
                chatPavilion.updateMemberInfo(info);
            }
            done(null);
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
        sendData(TaskTypes.SYNC_MEMBER, syncRoomUUID.getBytes(StandardCharsets.UTF_8));
    }
}
