package icu.mmmc.cr.callbacks;

/**
 * 进度回调类
 *
 * @author shouchen
 */
public interface ProgressCallback {
    /**
     * 进度开始
     */
    void start();

    /**
     * 进度更新
     *
     * @param status 进度
     * @param msg    进度提示
     */
    void update(double status, String msg);

    /**
     * 进度结束
     */
    void done();

    /**
     * 进度终止
     *
     * @param msg 错误信息
     */
    void halt(String msg);
}
