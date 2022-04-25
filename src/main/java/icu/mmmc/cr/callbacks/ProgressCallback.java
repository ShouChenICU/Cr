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
     */
    void update(double status);

    /**
     * 进度结束
     */
    void done();

    /**
     * 进度终止
     */
    void halt(String msg);
}
