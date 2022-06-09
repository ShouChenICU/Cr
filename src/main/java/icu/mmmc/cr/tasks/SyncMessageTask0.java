package icu.mmmc.cr.tasks;

import icu.mmmc.cr.ChatRoom;
import icu.mmmc.cr.ChatRoomManager;
import icu.mmmc.cr.constants.Constants;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.database.DaoManager;
import icu.mmmc.cr.entities.MessageInfo;
import icu.mmmc.cr.entities.RoomInfo;
import icu.mmmc.cr.utils.BsonObject;
import icu.mmmc.cr.utils.BsonUtils;
import org.bson.BSONObject;

import java.util.List;

/**
 * 消息同步任务（被动
 *
 * @author shouchen
 */
public class SyncMessageTask0 extends AbstractTask {
    private List<MessageInfo> messageInfoList;
    private int stepCount;

    public SyncMessageTask0() {
        super(null);
        stepCount = 0;
    }

    /**
     * 处理数据
     *
     * @param data 数据
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void handleData(byte[] data) throws Exception {
        BSONObject object = BsonUtils.deserialize(data);
        if (stepCount == 0) {
            String roomUUID = (String) object.get("ROOM_UUID");
            ChatRoom chatRoom = ChatRoomManager.getByRoomUUID(roomUUID);
            if (chatRoom == null) {
                // 如果找不到房间
                String err = "Room not found";
                sendError(err);
                halt(err);
                return;
            }
            if (!chatRoom.containMember(node.getNodeInfo().getUUID())) {
                // 如果成员不在该房间
                String err = "Member not found";
                sendError(err);
                halt(err);
                return;
            }
            RoomInfo roomInfo = chatRoom.getRoomInfo();
            messageInfoList = DaoManager.getMessageDao().getMessagesBeforeTime(roomInfo.getNodeUUID(), roomUUID, (long) object.get("TIMESTAMP"), Constants.SYNC_MESSAGES_QUANTITY);
            stepCount = 1;
            sendData(TaskTypes.ACK, null);
        } else {
            List<Integer> idList = (List<Integer>) object.get("ID_LIST");
            BsonObject obj = new BsonObject();
            for (MessageInfo info : messageInfoList) {
                if (!idList.contains(info.getId())) {
                    obj.set(String.valueOf(info.getId()), info.serialize());
                }
            }
            sendData(TaskTypes.ACK, obj.serialize());
            done();
        }
    }
}
