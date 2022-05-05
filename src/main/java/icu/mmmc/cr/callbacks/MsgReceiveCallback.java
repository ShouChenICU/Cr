package icu.mmmc.cr.callbacks;

import icu.mmmc.cr.entities.MessageInfo;

/**
 * 消息接收回调
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public interface MsgReceiveCallback {
    /**
     * 收到消息
     *
     * @param messageInfo 消息实体
     */
    void receiveMsg(MessageInfo messageInfo);
}
