package icu.mmmc.cr.tasks;

import icu.mmmc.cr.PacketBody;
import icu.mmmc.cr.callbacks.ProgressCallback;

/**
 * 初始化任务 被动
 *
 * @author shouchen
 */
public class InitTask0 extends AbstractTask {
    private int idCount;

    public InitTask0(ProgressCallback callback) {
        super(callback);
        idCount = 0;
    }

    /**
     * 处理包
     *
     * @param packetBody 包
     */
    @Override
    public void handlePacket(PacketBody packetBody) {

    }
}
