package icu.mmmc.cr.tasks;

import icu.mmmc.cr.Node;
import icu.mmmc.cr.callbacks.ProgressCallback;
import icu.mmmc.cr.constants.TaskTypes;
import icu.mmmc.cr.entities.Serialization;
import icu.mmmc.cr.utils.BsonObject;
import icu.mmmc.cr.utils.BsonUtils;

/**
 * 发起请求任务
 *
 * @author shouchen
 */
public class RequestTask extends AbstractRequestTask {
    private final String requestPath;
    private final Object[] requestArgs;

    public RequestTask(ProgressCallback callback, String requestPath, Object... args) {
        super(callback);
        this.requestPath = requestPath;
        this.requestArgs = args;
    }

    /**
     * 处理数据
     *
     * @param data 数据
     */
    @Override
    protected void handleData(byte[] data) throws Exception {
        done(BsonUtils.deserialize(data).get(RESULT));
    }

    @Override
    public void init(Node node, int taskId) {
        super.init(node, taskId);
        BsonObject object = new BsonObject().set(REQ_PATH, requestPath);
        for (int i = 0; i < requestArgs.length; i++) {
            Object param = requestArgs[i];
            if (param instanceof Serialization) {
                object.set(String.valueOf(i), ((Serialization) param).serialize());
            } else {
                object.set(String.valueOf(i), requestArgs[i]);
            }
        }
        sendData(TaskTypes.REQUEST, object.serialize());
    }
}
