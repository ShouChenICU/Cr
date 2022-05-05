package icu.mmmc.cr.tasks;

import icu.mmmc.cr.Node;
import icu.mmmc.cr.callbacks.ProgressCallback;

/**
 * 推送任务
 *
 * @author shouchen
 */
public class PushTask extends AbstractTask{
    public PushTask(ProgressCallback callback) {
        super(callback);
    }

    @Override
    public void init(Node node, int taskId) {
        super.init(node, taskId);
    }
}
