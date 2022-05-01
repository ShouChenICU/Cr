package icu.mmmc.cr;

import icu.mmmc.cr.entities.MemberInfo;
import icu.mmmc.cr.entities.MessageInfo;
import icu.mmmc.cr.entities.RoomInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 聊天室
 *
 * @author shouchen
 */
class ChatRoom {
    /**
     * 房间信息
     */
    private RoomInfo roomInfo;
    /**
     * 成员列表
     */
    private List<MemberInfo> memberList;
    /**
     * 在线节点
     */
    private List<Node> onlineNodeList;
    /**
     * 消息列表
     */
    private List<MessageInfo> messageList;

    public ChatRoom(RoomInfo roomInfo, List<MemberInfo> memberList) {
        this.roomInfo = roomInfo;
        this.memberList = memberList;
        onlineNodeList = new ArrayList<>();
        messageList = new ArrayList<>();
    }

    public RoomInfo getRoomInfo() {
        return roomInfo;
    }

    public ChatRoom setRoomInfo(RoomInfo roomInfo) {
        this.roomInfo = roomInfo;
        return this;
    }

    public List<MemberInfo> getMemberList() {
        return memberList;
    }

    public ChatRoom setMemberList(List<MemberInfo> memberList) {
        this.memberList = memberList;
        return this;
    }

    public List<MessageInfo> getMessageList() {
        return Collections.unmodifiableList(messageList);
    }
}
