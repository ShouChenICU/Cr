package icu.mmmc.cr.tasks;

import icu.mmmc.cr.Node;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.constants.RequestTypes;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.entities.MemberInfo;
import icu.mmmc.cr.entities.MessageInfo;
import icu.mmmc.cr.utils.BsonObject;

/**
 * 发起请求任务
 *
 * @author shouchen
 */
public class RequestTask extends AbstractTask {
    private final int requestType;
    private final Object[] requestArgs;

    public RequestTask(ProgressCallback callback, int requestType, Object... args) throws Exception {
        super(callback);
        switch (requestType) {
            case RequestTypes.ADD_MEMBER:
                // member info
                if (args.length != 1
                        || args[0].getClass() != MemberInfo.class) {
                    throw new Exception("Argument error");
                }
                break;
            case RequestTypes.DEL_MEMBER:
                // nodeUUID, roomUUID, memberUUID
            case RequestTypes.UPDATE_NICKNAME:
                // nickname, nodeUUID, roomUUID
                if (args.length != 3
                        || args[0].getClass() != String.class
                        || args[1].getClass() != String.class
                        || args[2].getClass() != String.class) {
                    throw new Exception("Argument error");
                }
                break;
            case RequestTypes.SEND_TEXT_MSG:
                // nodeUUID, roomUUID, messageInfo
                if (args.length != 3
                        || args[0].getClass() != String.class
                        || args[1].getClass() != String.class
                        || args[2].getClass() != MessageInfo.class) {
                    throw new Exception("Argument error");
                }
                break;
        }
        this.requestType = requestType;
        this.requestArgs = args;
    }

    /**
     * 处理数据
     *
     * @param data 数据
     */
    @Override
    protected void handleData(byte[] data) throws Exception {
        done();
    }

    @Override
    public void init(Node node, int taskId) {
        super.init(node, taskId);
        switch (requestType) {
            case RequestTypes.ADD_MEMBER:
                sendData(TaskTypes.REQUEST, new BsonObject()
                        .set("REQ_TYPE", requestType)
                        .set("0", ((MemberInfo) requestArgs[0]).serialize())
                        .serialize());
                break;
            case RequestTypes.DEL_MEMBER:
            case RequestTypes.UPDATE_NICKNAME:
                sendData(TaskTypes.REQUEST, new BsonObject()
                        .set("REQ_TYPE", requestType)
                        .set("0", requestArgs[0])
                        .set("1", requestArgs[1])
                        .set("2", requestArgs[2])
                        .serialize());
                break;
            case RequestTypes.SEND_TEXT_MSG:
                sendData(TaskTypes.REQUEST, new BsonObject()
                        .set("REQ_TYPE", requestType)
                        .set("0", requestArgs[0])
                        .set("1", requestArgs[1])
                        .set("2", ((MessageInfo) requestArgs[2]).serialize())
                        .serialize());
                break;
        }
    }
}
