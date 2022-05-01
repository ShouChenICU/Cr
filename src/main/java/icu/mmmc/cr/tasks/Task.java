package icu.mmmc.cr.tasks;

import icu.mmmc.cr.Node;
import icu.mmmc.cr.PacketBody;

/**
 * 任务接口
 *
 * @author shouchen
 */
public interface Task {
    /**
     * 初始化任务
     */
    void init(Node node, int taskId);

    /**
     * 处理包
     *
     * @param packetBody 包
     */
    void handlePacket(PacketBody packetBody);

    /**
     * 终止
     *
     * @param msg 错误信息
     */
    void halt(String msg);

    /**
     * 结束任务
     */
    void done();

    /**
     * 获取时间戳
     *
     * @return 时间戳
     */
    long getTimeStamp();
}
