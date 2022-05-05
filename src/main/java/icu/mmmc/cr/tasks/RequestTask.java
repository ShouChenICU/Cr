package icu.mmmc.cr.tasks;

import icu.mmmc.cr.Node;
import icu.mmmc.cr.callbacks.ProgressCallback;

/**
 * 发起请求任务
 *
 * @author shouchen
 */
public class RequestTask extends AbstractTask{
    public RequestTask(ProgressCallback callback) {
        super(callback);
    }

    @Override
    public void init(Node node, int taskId) {
        super.init(node, taskId);
    }
}
