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
    void handlePacket(PacketBody packetBody) throws Exception;

    /**
     * 终止任务
     *
     * @param msg 错误信息
     */
    void halt(String msg);

    /**
     * 结束任务
     */
    void done();

    /**
     * 获取开始时间戳
     *
     * @return 开始时间戳
     */
    long getStartTime();

    /**
     * 获取最后更新时间
     *
     * @return 最后更新时间戳
     */
    long getUpdateTime();
}
