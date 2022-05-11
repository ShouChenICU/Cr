package icu.mmmc.cr.exceptions;

/**
 * 未知实体类型异常
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class UnknownEntityTypeException extends Exception {
    public UnknownEntityTypeException() {
        super();
    }

    public UnknownEntityTypeException(String message) {
        super(message);
    }

    public UnknownEntityTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownEntityTypeException(Throwable cause) {
        super(cause);
    }

    protected UnknownEntityTypeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
