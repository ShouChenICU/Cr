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
    private static Selector selector;
    private static ServerSocketChannel serverSocketChannel;

    /**
     * 初始化网络核心
     *
     * @param port 监听端口，如果超出范围则不监听
     */
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
        Set<SelectionKey> selectionKeySet = selector.selectedKeys();
        while (isRun) {
            try {
                selector.select();
            } catch (Exception e) {
                Logger.error(e);
                halt();
            }
            Iterator<SelectionKey> iterator = selectionKeySet.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (key.isAcceptable()) {
                    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                    try {
                        SocketChannel channel = serverChannel.accept();
                        channel.configureBlocking(false);
                        SelectionKey key0 = channel.register(selector, 0);
                        // TODO: 2022/4/24
                    } catch (IOException e) {
                        Logger.warn(e);
                    }
                } else if (key.isReadable()) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    // TODO: 2022/4/24
                }
            }
        }
    }

    /**
     * 终止
     */
    public static synchronized void halt() {
        if (!isRun) {
            return;
        }
        Logger.info("halt");
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
        Logger.info("halt done");
    }
}
