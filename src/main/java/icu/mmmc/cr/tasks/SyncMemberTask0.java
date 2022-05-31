package icu.mmmc.cr.tasks;

import icu.mmmc.cr.ChatRoom;
import icu.mmmc.cr.ChatRoomManager;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.entities.MemberInfo;
import icu.mmmc.cr.utils.BsonObject;
import icu.mmmc.cr.utils.BsonUtils;
import org.bson.BasicBSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 成员同步任务（被动
 *
 * @author shouchen
 */
public class SyncMemberTask0 extends AbstractTask {
    // 最后新增的部分
    private final Map<String, MemberInfo> memberInfoMap;
    // 要更新的部分
    private final BsonObject update;
    private int stepCount;

    public SyncMemberTask0() {
        super(null);
        memberInfoMap = new HashMap<>();
        update = new BsonObject();
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
            List<MemberInfo> memberInfoList = chatRoom.getMemberList();
            for (MemberInfo info : memberInfoList) {
                memberInfoMap.put(info.getUserUUID(), info);
            }
            stepCount = 1;
            sendData(TaskTypes.ACK, null);
        } else if (stepCount == 1) {
            stepCount = 2;
            // 最后要删除的部分
            BasicBSONObject object = (BasicBSONObject) BsonUtils.deserialize(data);
            Iterator<Map.Entry<String, Object>> iterator = object.entrySet().iterator();
            while (iterator.hasNext()) {
                MemberInfo info1 = (MemberInfo) iterator.next();
                MemberInfo info2 = memberInfoMap.get(info1.getUserUUID());
                if (info2 != null) {
                    if (info1.getUpdateTime() != info2.getUpdateTime()) {
                        update.set(info2.getUserUUID(), info2.serialize());
                    }
                    memberInfoMap.remove(info1.getUserUUID());
                    iterator.remove();
                }
            }
            sendData(TaskTypes.ACK, BsonUtils.serialize(object));
        } else if (stepCount == 2) {
            stepCount = 3;
            sendData(TaskTypes.ACK, update.serialize());
        } else if (stepCount == 3) {
            BsonObject object = new BsonObject();
            for (Map.Entry<String, MemberInfo> entry : memberInfoMap.entrySet()) {
                object.set(entry.getKey(), entry.getValue().serialize());
            }
            sendData(TaskTypes.ACK, object.serialize());
            done();
        }
    }
}
