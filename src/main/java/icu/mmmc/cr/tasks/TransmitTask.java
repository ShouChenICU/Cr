package icu.mmmc.cr.tasks;

import icu.mmmc.cr.callbacks.ProgressCallback;

/**
 * 传输任务抽象类
 *
 * @author shouchen
 */
public abstract class TransmitTask extends AbstractTask {
    protected static final String ENTITY_TYPE = "ENTITY_TYPE";
    protected static final String DATA_LENGTH = "DATA_LENGTH";
    /**
     * 节点信息
     */
    protected static final int ENTITY_NODE_INFO = 1;
    /**
     * 房间信息
     */
    protected static final int ENTITY_ROOM_INFO = 2;
    /**
     * 成员信息
     */
    protected static final int ENTITY_MEMBER_INFO = 3;
    /**
     * 消息信息
     */
    protected static final int ENTITY_MESSAGE_INFO = 4;
    /**
     * 传输实体类型
     */
    protected int entityType;
    /**
     * 已处理的数据长度
     */
    protected int processedLength;
    /**
     * 数据
     */
    protected byte[] data;

    public TransmitTask(ProgressCallback callback) {
        super(callback);
        entityType = 0;
        processedLength = 0;
    }
}
