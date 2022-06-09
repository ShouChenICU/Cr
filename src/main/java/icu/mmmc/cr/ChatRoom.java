package icu.mmmc.cr;

import icu.mmmc.cr.callbacks.MsgReceiveCallback;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.entities.MemberInfo;
import icu.mmmc.cr.entities.MessageInfo;
import icu.mmmc.cr.entities.RoomInfo;

import java.util.List;

/**
 * 聊天室接口
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public interface ChatRoom {
    /**
     * 获取房间信息
     *
     * @return 房间信息
     */
    RoomInfo getRoomInfo();

    /**
     * 添加成员
     *
     * @param UUID 成员标识码
     */
    void addMember(String UUID) throws Exception;

    /**
     * 移除成员
     *
     * @param UUID 成员标识码
     */
    void removeMember(String UUID) throws Exception;

    /**
     * 是否存在成员
     *
     * @param uuid 成员标识码
     * @return 如果存在该成员则返回true, 否则返回false
     */
    boolean containMember(String uuid);

    /**
     * 获取成员信息
     *
     * @param uuid 成员标识码
     * @return 成员信息，没有则返回null
     */
    MemberInfo getMemberInfo(String uuid);

    /**
     * 获取成员列表
     *
     * @return 成员列表
     */
    List<MemberInfo> getMemberList();

    /**
     * 获取成员数量
     *
     * @return 成员数量
     */
    int getMemberCount();

    /**
     * 更新自己的昵称
     *
     * @param nickname 昵称
     */
    void updateNickname(String nickname) throws Exception;

    /**
     * 更新自己的头衔
     *
     * @param label 头衔
     */
    void updateLabel(String label) throws Exception;

    /**
     * 更新房间名称
     *
     * @param title 房间名
     */
    void updateRoomTitle(String title) throws Exception;

    /**
     * 获取消息列表
     *
     * @return 消息列表
     */
    List<MessageInfo> getMessageList();

    /**
     * 获取最新消息
     *
     * @return 最新消息
     */
    MessageInfo getLatestMessage();

    /**
     * 获取未读消息数量
     *
     * @return 未读消息数
     */
    int getUnreadCount();

    /**
     * 设置未读消息数量
     *
     * @param unreadCount 未读消息数量
     */
    void setUnreadCount(int unreadCount);

    /**
     * 发送文本消息
     *
     * @param content  文本消息内容
     * @param callback 进度回调
     */
    void postMessage(String content, ProgressCallback callback);

    /**
     * 授权管理员权限
     *
     * @param userUUID 成员标识码
     */
    void grantAdminPermission(String userUUID) throws Exception;

    /**
     * 撤销管理员权限
     *
     * @param userUUID 成员标识码
     */
    void revokeAdminPermission(String userUUID) throws Exception;

    /**
     * 同步指定时间之前的消息列表
     *
     * @param earliestTime 时间
     * @param callback     进度回调
     */
    void syncMessagesBeforeTime(long earliestTime, ProgressCallback callback);

    /**
     * 同步成员列表
     *
     * @param callback 进度回调
     */
    void syncMembers(ProgressCallback callback);

    /**
     * 是否在线
     *
     * @return 如果房间主人是自己，直接返回true
     * 如果房间主人在线，则返回true
     * 否则返回false
     */
    boolean isOnline();

    /**
     * 是否是管理员
     *
     * @return 如果房间是自己管理，则为=返回true,否则返回false
     */
    boolean isOwner();

    /**
     * 设置消息接收回调
     *
     * @param msgReceiveCallback 消息接收回调
     */
    void setMsgReceiveCallback(MsgReceiveCallback msgReceiveCallback);
}
