package icu.mmmc.cr;

import icu.mmmc.cr.utils.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

/**
 * 网络核心类
 *
 * @author shouchen
 */
class NetCore {
    private static volatile boolean isRun = false;
    private static Selector selector;
    private static ServerSocketChannel serverSocketChannel;

    public static synchronized void init(int port) throws Exception {
        if (isRun) {
            return;
        }
        Logger.info("init");
        selector = Selector.open();
        if (port >= 0 && port < 65536) {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        }
        isRun = true;
        Thread thread = new Thread(NetCore::loop);
        thread.setDaemon(true);
        thread.start();
        Logger.info("init done");
    }

    private static void loop() {
        Logger.info("start loop");
        while (isRun) {
            // TODO: 2022/4/22
        }
    }

    public static boolean isRun() {
        return isRun;
    }

    public static synchronized void halt() {
        if (!isRun) {
            return;
        }
        Logger.info("stop");
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
        } catch (IOException e) {
            Logger.warn(e);
        }
        selector = null;
        Logger.info("stop done");
    }
}
