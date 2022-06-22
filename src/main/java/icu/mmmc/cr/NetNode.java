package icu.mmmc.cr;

import icu.mmmc.cr.utils.Logger;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeoutException;

/**
 * 网络节点
 * 处理网络底层收发包逻辑
 *
 * @author shouchen
 */
abstract class NetNode {
    private static final int TIME_OUT_LENGTH = 100;
    private static final int TIME_OUT_COUNT = 5;
    private static final int BUFFER_SIZE = 4096;
    protected final SelectionKey key;
    private final Object keyLock;
    protected final long createTime;
    protected final ByteBuffer readBuffer;
    protected final ByteBuffer writeBuffer;
    protected long writeLength;
    protected long readLength;
    private int packetStatus;
    private int packetLength;
    private byte[] packetData;

    public NetNode(SelectionKey key) {
        this.createTime = System.currentTimeMillis();
        this.keyLock = new Object();
        this.writeLength = 0;
        this.readLength = 0;
        this.key = key;
        this.readBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        this.writeBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    }

    /**
     * 写操作
     */
    @SuppressWarnings("BusyWait")
    public void doWrite(byte[] data) {
        int length = data.length;
        writeLength += length;
        Logger.debug("Do write, data length = " + length);
        try {
            if (length > 65535) {
                throw new Exception("Data length too long " + length);
            }
            SocketChannel channel = (SocketChannel) key.channel();
            synchronized (keyLock) {
                writeBuffer.clear();
                writeBuffer.put((byte) (length % 256))
                        .put((byte) (length / 256));
                int offset = 0;
                while (offset < length) {
                    int len = Math.min(writeBuffer.remaining(), length - offset);
                    writeBuffer.put(data, offset, len);
                    offset += len;
                    writeBuffer.flip();
                    int waitCount = 0;
                    while (writeBuffer.hasRemaining()) {
                        if (channel.write(writeBuffer) == 0) {
                            if (waitCount >= TIME_OUT_COUNT) {
                                throw new TimeoutException("Write time out!");
                            }
                            Thread.sleep(TIME_OUT_LENGTH);
                            waitCount++;
                        } else {
                            waitCount = 0;
                        }
                    }
                    writeBuffer.clear();
                }
            }
        } catch (Exception e) {
            exceptionHandler(e);
        }
    }

    /**
     * 断开连接
     *
     * @param reason 原因
     */
    public void disconnect(String reason) throws Exception {
        if (key != null) {
            key.cancel();
            key.selector().wakeup();
            key.channel().close();
        }
    }

    /**
     * 是否连接
     */
    public boolean isConnect() {
        return key != null && key.isValid() && ((SocketChannel) key.channel()).isConnected();
    }

    /**
     * 读操作
     */
    public void doRead() {
        try {
            int len;
            SocketChannel channel = (SocketChannel) key.channel();
            while (true) {
                if (!channel.isConnected()) {
                    return;
                }
                len = channel.read(readBuffer);
                if (len == 0) {
                    break;
                }
                if (len == -1) {
                    Logger.debug("Channel closed");
                    disconnect("Channel closed");
                    return;
                }
                readLength += len;
                readBuffer.flip();
                while (readBuffer.hasRemaining()) {
                    switch (packetStatus) {
                        case 0:
                            packetLength = readBuffer.get() & 0xff;
                            packetStatus = 1;
                            break;
                        case 1:
                            packetLength += (readBuffer.get() & 0xff) << 8;
                            packetData = new byte[packetLength];
                            packetLength = 0;
                            packetStatus = 2;
                            break;
                        case 2:
                            if (packetLength < packetData.length) {
                                len = Math.min(readBuffer.remaining(), packetData.length - packetLength);
                                readBuffer.get(packetData, packetLength, len);
                                packetLength += len;
                            }
                            if (packetLength == packetData.length) {
                                dataHandler(packetData);
                                packetStatus = 0;
                            }
                            break;
                    }
                }
                readBuffer.clear();
            }
            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
            key.selector().wakeup();
        } catch (Exception e) {
            exceptionHandler(e);
        }
    }

    /**
     * 数据处理
     *
     * @param data 数据
     */
    protected abstract void dataHandler(byte[] data) throws Exception;

    /**
     * 异常处理
     */
    protected abstract void exceptionHandler(Exception exception);
}
