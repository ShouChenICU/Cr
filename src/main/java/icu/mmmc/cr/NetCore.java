package icu.mmmc.cr;

import icu.mmmc.cr.utils.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 网络核心类
 *
 * @author shouchen
 */
class NetCore {
    private static volatile boolean isRun = false;
    private static final Object REG_LOCK = new Object();
    private static Selector selector;
    private static ServerSocketChannel serverSocketChannel;
    private static Thread loopThread;

    /**
     * 初始化网络核心
     *
     * @param port 监听端口，如果超出范围则不监听
     */
    public static synchronized void init(int port) throws Exception {
        if (isRun) {
            return;
        }
        Logger.info("Init");
        selector = Selector.open();
        if (port >= 0 && port < 65536) {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        }
        isRun = true;
        if (loopThread != null && loopThread.isAlive()) {
            loopThread.interrupt();
        }
        loopThread = new Thread(NetCore::loop);
        loopThread.setName("NetLoop-thread");
        loopThread.start();
        Logger.info("Init done");
    }

    /**
     * 网络轮询
     */
    private static void loop() {
        Logger.info("Start loop");
        Set<SelectionKey> selectionKeySet = selector.selectedKeys();
        while (isRun) {
            try {
                synchronized (REG_LOCK) {
                    Logger.debug("Select");
                }
                selector.select();
            } catch (Exception e) {
                Logger.error(e);
                halt();
                return;
            }
            Iterator<SelectionKey> iterator = selectionKeySet.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                try {
                    if (key.isAcceptable()) {
                        Logger.debug("Accept");
                        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                        SocketChannel channel = serverChannel.accept();
                        channel.configureBlocking(false);
                        SelectionKey key0 = channel.register(selector, 0);
                        try {
                            NodeManager.acceptNode(key0, null);
                        } catch (Exception e) {
                            Logger.warn(e);
                            channel.close();
                            key0.cancel();
                        }
                        continue;
                    }
                    if (key.isReadable()) {
                        Logger.debug("Read");
                        key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
                        WorkerThreadPool.execute(() -> ((Node) key.attachment()).doRead());
                    }
                    if (key.isWritable()) {
                        Logger.debug("Write");
                        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                        WorkerThreadPool.execute(() -> ((Node) key.attachment()).doPost());
                    }
                } catch (Exception e) {
                    if (key.channel() == serverSocketChannel) {
                        Logger.error(e);
                        halt();
                        return;
                    }
                    Logger.warn(e);
                }
            }
        }
    }

    /**
     * 注册通道
     *
     * @param channel 通道
     * @return SelectionKey
     */
    public static SelectionKey register(SocketChannel channel) throws Exception {
        synchronized (REG_LOCK) {
            Logger.debug("Register");
            selector.wakeup();
            return channel.register(selector, 0);
        }
    }

    /**
     * 终止
     */
    public static synchronized void halt() {
        if (!isRun) {
            return;
        }
        Logger.info("Halt");
        if (serverSocketChannel != null) {
            try {
                serverSocketChannel.close();
            } catch (IOException e) {
                Logger.warn(e);
            }
            serverSocketChannel = null;
        }
        isRun = false;
        try {
            selector.close();
            loopThread.join(1000);
        } catch (Exception e) {
            Logger.warn(e);
        }
        selector = null;
        loopThread = null;
        Logger.info("Halt done");
    }
}
