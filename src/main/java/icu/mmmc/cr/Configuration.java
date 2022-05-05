package icu.mmmc.cr;

/**
 * 核心配置类
 *
 * @author shouchen
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Configuration {
    private static final int DEFAULT_PORT = 4224;
    /**
     * 是否监听连接请求
     */
    private boolean isListen;
    /**
     * 监听端口
     */
    private int listenPort;
    /**
     * 工作线程池大小
     */
    private int workerThreadPoolSize;
    /**
     * 日志记录等级
     * 0 DEBUG
     * 1 INFO
     * 2 WARN
     * 3 ERROR
     */
    private int logLevel;

    public Configuration() {
        isListen = true;
        listenPort = DEFAULT_PORT;
        workerThreadPoolSize = Runtime.getRuntime().availableProcessors();
        logLevel = 1;
    }

    /**
     * 检查配置完整性
     */
    public void check() throws Exception {
        if (listenPort < 1 || listenPort > 65535) {
            throw new Exception("端口范围错误(1-65535): " + listenPort);
        }
        if (workerThreadPoolSize < 1) {
            throw new Exception("工作线程池大小必须大于0: " + workerThreadPoolSize);
        }
    }

    public boolean isListen() {
        return isListen;
    }

    public Configuration setListen(boolean listen) {
        isListen = listen;
        return this;
    }

    public int getListenPort() {
        return listenPort;
    }

    public Configuration setListenPort(int listenPort) {
        this.listenPort = listenPort;
        return this;
    }

    public int getWorkerThreadPoolSize() {
        return workerThreadPoolSize;
    }

    public Configuration setWorkerThreadPoolSize(int workerThreadPoolSize) {
        this.workerThreadPoolSize = workerThreadPoolSize;
        return this;
    }

    public int getLogLevel() {
        return logLevel;
    }

    public Configuration setLogLevel(int logLevel) {
        this.logLevel = logLevel;
        return this;
    }
}
