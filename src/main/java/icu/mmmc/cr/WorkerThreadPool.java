package icu.mmmc.cr;

import icu.mmmc.cr.utils.Logger;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 任务线程池
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class WorkerThreadPool {
    private static final int MAX_WORK_COUNT = 4096;
    private static final String THREAD_NAME = "worker-thread";
    private static ThreadPoolExecutor executor = null;
    private static volatile int threadNum = 0;

    /**
     * 初始化任务线程池
     *
     * @param threadCount 线程数量
     */
    public static synchronized void init(int threadCount) {
        if (executor != null) {
            return;
        }
        executor = new ThreadPoolExecutor(threadCount, threadCount, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(MAX_WORK_COUNT), runnable -> {
            Thread thread = new Thread(runnable);
            synchronized (WorkerThreadPool.class) {
                thread.setName(THREAD_NAME + "-" + threadNum++);
            }
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.CallerRunsPolicy() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                Logger.warn("线程池任务数已满！");
                super.rejectedExecution(r, e);
            }
        });
    }

    /**
     * 加入任务
     *
     * @param runnable 待执行任务
     */
    public static void execute(Runnable runnable) {
        try {
            executor.execute(runnable);
        } catch (Exception e) {
            Logger.warn(e);
        }
    }

    /**
     * 关闭任务线程池
     */
    public static void halt() {
        Logger.info("stop");
        try {
            executor.shutdown();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                Logger.warn("任务线程池非正常关闭");
            }
        } catch (InterruptedException e) {
            Logger.error(e);
        } finally {
            executor = null;
            threadNum = 0;
        }
    }
}
