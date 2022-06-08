package icu.mmmc.cr.database.adapters;

import icu.mmmc.cr.database.interfaces.MessageDao;
import icu.mmmc.cr.entities.MessageInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息数据访问适配器
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class MessageDaoAdapter implements MessageDao {
    /**
     * 从数据库删除指定时间之前的消息
     *
     * @param nodeUUID  节点标识码
     * @param roomUUID  房间标识码
     * @param timestamp 时间戳
     */
    @Override
    public void deleteMessagesBefore(String nodeUUID, String roomUUID, long timestamp) {
    }

    /**
     * 向数据库添加一个消息记录
     *
     * @param messageInfo 消息实体
     */
    @Override
    public void putMessage(MessageInfo messageInfo) {
    }

    /**
     * 获取最大的id
     *
     * @param nodeUUID 节点标识码
     * @param roomUUID 房间标识码
     * @return id
     */
    @Override
    public int getMaxID(String nodeUUID, String roomUUID) {
        return 0;
    }

    /**
     * 获取指定房间的指定时间之前的指定数量的消息列表
     *
     * @param nodeUUID  节点标识码
     * @param roomUUID  房间标识码
     * @param timeStamp 时间戳
     * @param count     数量
     * @return 消息列表
     */
    @Override
    public List<MessageInfo> getMessagesBeforeTime(String nodeUUID, String roomUUID, long timeStamp, int count) {
        return new ArrayList<>();
    }
}
