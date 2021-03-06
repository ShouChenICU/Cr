package icu.mmmc.cr.constants;

/**
 * 任务类型
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class TaskTypes {
    /**
     * 断开连接
     */
    public static final int DISCONNECT = -2;
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
     * 发起请求
     */
    public static final int REQUEST = 5;
    /**
     * 房间同步
     */
    public static final int SYNC_ROOM = 6;
    /**
     * 成员同步
     */
    public static final int SYNC_MEMBER = 7;
    /**
     * 消息同步
     */
    public static final int SYNC_MESSAGE = 8;
}
