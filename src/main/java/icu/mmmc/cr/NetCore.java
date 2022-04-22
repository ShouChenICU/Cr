package icu.mmmc.cr;

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
        selector = Selector.open();
        if (port >= 0 && port < 65536) {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        }
    }

    private static void loop() {
        while (isRun) {
            // TODO: 2022/4/22
        }
    }

    public static boolean isRun() {
        return isRun;
    }

    public static void close() {
        // TODO: 2022/4/22
    }
}
