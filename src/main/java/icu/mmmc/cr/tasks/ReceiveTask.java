package icu.mmmc.cr.tasks;

import icu.mmmc.cr.PacketBody;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.utils.BsonUtils;
import org.bson.BSONObject;

import java.nio.charset.StandardCharsets;

/**
 * 接收任务
 *
 * @author shouchen
 */
public class ReceiveTask extends TransmitTask {
    private int stepCount;

    public ReceiveTask() {
        super(null);
        stepCount = 0;
    }

    /**
     * 处理包
     *
     * @param packetBody 包
     */
    @Override
    public void handlePacket(PacketBody packetBody) throws Exception {
        super.handlePacket(packetBody);
        if (stepCount == 0) {
            BSONObject object = BsonUtils.deserialize(packetBody.getPayload());
            entityType = (int) object.get("ENTITY_TYPE");
            data = new byte[(int) object.get("DATA_LENGTH")];
            if (entityType != ENTITY_NODE_INFO
                    && entityType != ENTITY_ROOM_INFO
                    && entityType != ENTITY_MEMBER_INFO
                    && entityType != ENTITY_MESSAGE_INFO) {
                String s = "unknown entity type";
                node.postPacket(
                        new PacketBody()
                                .setSource(taskId)
                                .setDestination(packetBody.getSource())
                                .setTaskType(TaskTypes.ERROR)
                                .setPayload(s.getBytes(StandardCharsets.UTF_8))
                );
                halt(s);
                return;
            }
            stepCount = 1;
        } else {
            byte[] buf = packetBody.getPayload();
            System.arraycopy(buf, 0, data, processedLength, buf.length);
            processedLength += buf.length;
            if (processedLength == data.length) {
                done();
            }
        }
        node.postPacket(
                new PacketBody()
                        .setSource(taskId)
                        .setDestination(packetBody.getSource())
                        .setTaskType(TaskTypes.ACK)
        );
    }

    @Override
    public void done() {
        switch (entityType) {
            case ENTITY_NODE_INFO:
                break;
            case ENTITY_ROOM_INFO:
                break;
            case ENTITY_MEMBER_INFO:
                break;
            case ENTITY_MESSAGE_INFO:
                break;
        }
        super.done();
    }
}
