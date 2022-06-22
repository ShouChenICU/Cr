package icu.mmmc.cr.tasks;

import icu.mmmc.cr.ChatPavilion;
import icu.mmmc.cr.ChatRoom;
import icu.mmmc.cr.Cr;
import icu.mmmc.cr.Node;
import icu.mmmc.cr.callbacks.MsgReceiveCallback;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.constants.Constants;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.database.DaoManager;
import icu.mmmc.cr.entities.MessageInfo;
import icu.mmmc.cr.entities.RoomInfo;
import icu.mmmc.cr.utils.BsonObject;
import icu.mmmc.cr.utils.BsonUtils;
import icu.mmmc.cr.utils.Logger;
import org.bson.BasicBSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息同步任务（主动
 *
 * @author shouchen
 */
public class SyncMessageTask1 extends AbstractTask {
    private final ChatPavilion chatPavilion;
    private final RoomInfo roomInfo;
    private final long timeStamp;
    private final List<MessageInfo> messageInfoList;
    private int stepCount;

    public SyncMessageTask1(ChatRoom chatRoom, long timeStamp, ProgressCallback callback) {
        super(callback);
        this.chatPavilion = (ChatPavilion) chatRoom;
        this.roomInfo = chatRoom.getRoomInfo();
        this.timeStamp = timeStamp;
        messageInfoList = DaoManager.getMessageDao().getMessagesBeforeTime(roomInfo.getNodeUUID(), roomInfo.getRoomUUID(), timeStamp, Constants.SYNC_MESSAGES_QUANTITY);
        for (MessageInfo info : messageInfoList) {
            try {
                chatPavilion.putMessage(info);
            } catch (Exception e) {
                Logger.warn(e);
            }
        }
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
            stepCount = 1;
            List<Integer> idList = new ArrayList<>();
            for (MessageInfo info : messageInfoList) {
                idList.add(info.getId());
            }
            sendData(TaskTypes.ACK, new BsonObject().set("ID_LIST", idList).serialize());
        } else {
            BasicBSONObject object = (BasicBSONObject) BsonUtils.deserialize(data);
            for (Object dat : object.values()) {
                MessageInfo messageInfo = new MessageInfo((byte[]) dat);
                chatPavilion.putMessage(messageInfo);
                DaoManager.getMessageDao().putMessage(messageInfo);
                MsgReceiveCallback callback = Cr.CallBack.msgReceiveCallback;
                if (callback != null) {
                    callback.receiveMsg(chatPavilion, messageInfo);
                }
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
        sendData(TaskTypes.SYNC_MESSAGE, new BsonObject()
                .set("ROOM_UUID", roomInfo.getRoomUUID())
                .set("TIMESTAMP", timeStamp)
                .serialize());
    }
}
