package icu.mmmc.cr.constants;

/**
 * 任务类型
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class TaskTypes {
    /**
     * 出错
     */
    public static final int ERROR = -1;
    /**
     * ping
     */
    public static final int PING = 0;
    /**
     * pong
     */
    public static final int PONG = 1;
    /**
     * 确认
     */
    public static final int ACK = 2;
    /**
     * 初始化
     */
    public static final int INIT = 3;
    /**
     * 推送
     */
    public static final int PUSH = 4;
    /**
     * 房间同步
     */
    public static final int SYNC_ROOM = 5;
    /**
     * 成员同步
     */
    public static final int SYNC_MEMBER = 6;
    /**
     * 消息同步
     */
    public static final int SYNC_MESSAGE = 7;
}
