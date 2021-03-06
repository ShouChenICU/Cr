package icu.mmmc.cr.utils;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日志记录类
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public final class Logger {
    public static final int DEBUG = 0;
    public static final int INFO = 1;
    public static final int WARN = 2;
    public static final int ERROR = 3;
    private static final Map<Long, DateFormat> DATE_FORMAT_MAP;
    private static final PrintStream CONSOLE_OUT;
    private static PrintStream fileOut;
    private static int level;

    static {
        DATE_FORMAT_MAP = new ConcurrentHashMap<>();
        level = 1;
        CONSOLE_OUT = System.out;
        fileOut = null;
    }

    /**
     * 设置日志等级
     * 0 DEBUG
     * 1 INFO
     * 2 WARN
     * 3 ERROR
     *
     * @param level 等级
     */
    public static void setLevel(int level) {
        Logger.level = level;
    }

    /**
     * 设置日志文件输出流
     *
     * @param fileOut 文件输出
     */
    public static void setFileOut(PrintStream fileOut) {
        synchronized (Logger.class) {
            Logger.fileOut = fileOut;
        }
    }

    /**
     * 输出日志
     *
     * @param lv  日志等级
     * @param msg 日志消息
     */
    private static void log(int lv, Object msg) {
        StringBuilder builder = new StringBuilder(getTimeStr());
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        StackTraceElement element = elements[elements.length > 3 ? 3 : elements.length - 1];
        builder.append(" [\033[35m");
        builder.append(Thread.currentThread().getName());
        builder.append("\033[0m] (");
        builder.append(element.getClassName());
        builder.append(":\033[36m");
        builder.append(element.getLineNumber());
        switch (lv) {
            case 0:
                builder.append("\033[0m) \033[34mDEBUG\033[0m -> ");
                break;
            case 1:
                builder.append("\033[0m) \033[32mINFO\033[0m -> ");
                break;
            case 2:
                builder.append("\033[0m) \033[33mWARN\033[0m -> ");
                break;
            case 3:
                builder.append("\033[0m) \033[31mERROR\033[0m -> ");
                break;
        }
        if (msg instanceof Exception) {
            builder.append(Objects.requireNonNullElse(((Exception) msg).getMessage(), msg.toString()));
            synchronized (Logger.class) {
                CONSOLE_OUT.println(builder);
                ((Exception) msg).printStackTrace(CONSOLE_OUT);
                if (fileOut != null) {
                    fileOut.println(builder);
                    ((Exception) msg).printStackTrace(fileOut);
                    fileOut.flush();
                }
            }
        } else {
            builder.append(msg);
            synchronized (Logger.class) {
                CONSOLE_OUT.println(builder);
                if (fileOut != null) {
                    fileOut.println(builder);
                    fileOut.flush();
                }
            }
        }
    }

    /**
     * 错误日志
     *
     * @param msg 日志消息
     */
    public static void error(Object msg) {
        if (msg == null || level > 3) {
            return;
        }
        log(3, msg);
    }

    /**
     * 警告日志
     *
     * @param msg 日志消息
     */
    public static void warn(Object msg) {
        if (msg == null || level > 2) {
            return;
        }
        log(2, msg);
    }

    /**
     * 普通日志
     *
     * @param msg 日志消息
     */
    public static void info(Object msg) {
        if (msg == null || level > 1) {
            return;
        }
        log(1, msg);
    }

    /**
     * 测试日志
     *
     * @param msg 日志消息
     */
    public static void debug(Object msg) {
        if (msg == null || level > 0) {
            return;
        }
        log(0, msg);
    }

    /**
     * 获取当前时间的格式化字符串
     *
     * @return 时间格式化字符串
     */
    private static String getTimeStr() {
        long id = Thread.currentThread().getId();
        DateFormat dateFormat = DATE_FORMAT_MAP.get(id);
        if (dateFormat == null) {
            dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            DATE_FORMAT_MAP.put(id, dateFormat);
        }
        return dateFormat.format(new Date());
    }
}
