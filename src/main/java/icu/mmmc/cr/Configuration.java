package icu.mmmc.cr;

import icu.mmmc.cr.entities.Checkable;
import icu.mmmc.cr.entities.Serialization;
import icu.mmmc.cr.utils.BsonObject;
import icu.mmmc.cr.utils.BsonUtils;
import icu.mmmc.cr.utils.Logger;
import org.bson.BSONObject;

/**
 * 核心配置类
 *
 * @author shouchen
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Configuration implements Serialization, Checkable {
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
        // 默认启用监听
        isListen = true;
        // 默认监听端口4224
        listenPort = DEFAULT_PORT;
        // 工作线程池大小默认设置为CPU线程数
        workerThreadPoolSize = Runtime.getRuntime().availableProcessors();
        // 日志默认设置为INFO级
        logLevel = Logger.INFO;
    }

    public Configuration(byte[] dat) {
        BSONObject object = BsonUtils.deserialize(dat);
        isListen = (boolean) object.get("IS_LISTEN");
        listenPort = (int) object.get("LISTEN_PORT");
        workerThreadPoolSize = (int) object.get("WORKER_THREAD_POOL_SIZE");
        logLevel = (int) object.get("LOG_LEVEL");
    }

    /**
     * 检查配置完整性
     */
    @Override
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

    /**
     * 序列化为Bson数据
     *
     * @return 序列化数据
     */
    @Override
    public byte[] serialize() {
        return new BsonObject()
                .set("IS_LISTEN", isListen)
                .set("LISTEN_PORT", listenPort)
                .set("WORKER_THREAD_POOL_SIZE", workerThreadPoolSize)
                .set("LOG_LEVEL", logLevel)
                .serialize();
    }
}
