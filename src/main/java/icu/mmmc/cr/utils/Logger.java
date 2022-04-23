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
@SuppressWarnings({"DuplicatedCode", "unused"})
public final class Logger {
    public static final int DEBUG = 0;
    public static final int INFO = 1;
    public static final int WARN = 2;
    public static final int ERROR = 3;
    private static final Map<Long, DateFormat> DATE_FORMAT_MAP;
    private static int level;
    private static PrintStream out;

    static {
        DATE_FORMAT_MAP = new ConcurrentHashMap<>();
        level = 1;
        out = System.out;
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

    public static void setOut(PrintStream out) {
        Logger.out = out;
    }

    public static void error(Object msg) {
        if (msg == null || level > 3) {
            return;
        }
        StringBuilder builder = new StringBuilder(getTimeStr());
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        StackTraceElement element = elements[elements.length > 1 ? 2 : elements.length - 1];
        builder.append(" [\033[35m");
        builder.append(Thread.currentThread().getName());
        builder.append("\033[0m] (");
        builder.append(element.getClassName());
        builder.append(":\033[36m");
        builder.append(element.getLineNumber());
        builder.append("\033[0m) \033[31mERROR\033[0m -> ");
        if (msg instanceof Exception) {
            builder.append(Objects.requireNonNullElse(((Exception) msg).getMessage(), msg.toString()));
            synchronized (Logger.class) {
                out.println(builder);
            }
            ((Exception) msg).printStackTrace(out);
        } else {
            builder.append(msg);
            synchronized (Logger.class) {
                out.println(builder);
            }
        }
    }

    public static void warn(Object msg) {
        if (msg == null || level > 2) {
            return;
        }
        StringBuilder builder = new StringBuilder(getTimeStr());
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        StackTraceElement element = elements[elements.length > 1 ? 2 : elements.length - 1];
        builder.append(" [\033[35m");
        builder.append(Thread.currentThread().getName());
        builder.append("\033[0m] (");
        builder.append(element.getClassName());
        builder.append(":\033[36m");
        builder.append(element.getLineNumber());
        builder.append("\033[0m) \033[33mWARN\033[0m -> ");
        if (msg instanceof Exception) {
            builder.append(Objects.requireNonNullElse(((Exception) msg).getMessage(), msg.toString()));
            synchronized (Logger.class) {
                out.println(builder);
            }
            ((Exception) msg).printStackTrace(out);
        } else {
            builder.append(msg);
            synchronized (Logger.class) {
                out.println(builder);
            }
        }
    }

    public static void info(Object msg) {
        if (msg == null || level > 1) {
            return;
        }
        StringBuilder builder = new StringBuilder(getTimeStr());
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        StackTraceElement element = elements[elements.length > 1 ? 2 : elements.length - 1];
        builder.append(" [\033[35m");
        builder.append(Thread.currentThread().getName());
        builder.append("\033[0m] (");
        builder.append(element.getClassName());
        builder.append(":\033[36m");
        builder.append(element.getLineNumber());
        builder.append("\033[0m) \033[32mINFO\033[0m -> ");
        if (msg instanceof Exception) {
            builder.append(Objects.requireNonNullElse(((Exception) msg).getMessage(), msg.toString()));
            synchronized (Logger.class) {
                out.println(builder);
            }
            ((Exception) msg).printStackTrace(out);
        } else {
            builder.append(msg);
            synchronized (Logger.class) {
                out.println(builder);
            }
        }
    }

    public static void debug(Object msg) {
        if (msg == null || level > 0) {
            return;
        }
        StringBuilder builder = new StringBuilder(getTimeStr());
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        StackTraceElement element = elements[elements.length > 1 ? 2 : elements.length - 1];
        builder.append(" [\033[35m");
        builder.append(Thread.currentThread().getName());
        builder.append("\033[0m] (");
        builder.append(element.getClassName());
        builder.append(":\033[36m");
        builder.append(element.getLineNumber());
        builder.append("\033[0m) \033[34mDEBUG\033[0m -> ");
        if (msg instanceof Exception) {
            builder.append(Objects.requireNonNullElse(((Exception) msg).getMessage(), msg.toString()));
            synchronized (Logger.class) {
                out.println(builder);
            }
            ((Exception) msg).printStackTrace(out);
        } else {
            builder.append(msg);
            synchronized (Logger.class) {
                out.println(builder);
            }
        }
    }

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
