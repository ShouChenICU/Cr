package icu.mmmc.cr;

/**
 * 连接引导器
 * 发起或接受连接，并协商密钥和初始化操作
 *
 * @author shouchen
 */
class ConnectionGuider {
    private static final int PASSIVE = 0;
    private static final int POSITIVE = 1;
    /**
     * 连接模式
     * 0 被动连接
     * 1 主动连接
     */
    private int mode;

    public ConnectionGuider() {

    }
}
