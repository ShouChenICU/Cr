package icu.mmmc.cr.callbacks.adapters;

import icu.mmmc.cr.callbacks.ProgressCallback;

public class ProgressAdapter implements ProgressCallback {
    /**
     * 进度开始
     */
    @Override
    public void start() {
    }

    /**
     * 进度更新
     *
     * @param status 进度
     * @param msg    进度提示
     */
    @Override
    public void update(double status, String msg) {
    }

    /**
     * 进度结束
     */
    @Override
    public void done() {
    }

    /**
     * 进度终止
     *
     * @param msg 错误信息
     */
    @Override
    public void halt(String msg) {
    }
}
