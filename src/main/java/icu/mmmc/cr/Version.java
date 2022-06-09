package icu.mmmc.cr;

/**
 * 版本号
 *
 * @author shouchen
 */
public final class Version {
    private static final String[] EDITIONS = new String[]{"Alpha", "Beta", "Release"};
    /**
     * 主版本号
     */
    public static final Integer MAJOR_VERSION = 0;
    /**
     * 副版本号
     */
    public static final Integer MINOR_VERSION = 1;
    /**
     * 修订号
     */
    public static final Integer PATCH_VERSION = 71;
    /**
     * 通讯协议版本
     */
    public static final Integer PROTOCOL_VERSION = 1;
    /**
     * 版本类型
     */
    public static final String EDITION = EDITIONS[0];
    /**
     * 版本全称
     */
    public static final String VERSION_STRING = MAJOR_VERSION + "." + MINOR_VERSION + "." + PATCH_VERSION + "-" + EDITION;
}
