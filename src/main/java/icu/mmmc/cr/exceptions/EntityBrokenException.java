package icu.mmmc.cr.exceptions;

/**
 * 实体信息损坏异常
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class EntityBrokenException extends Exception {
    public EntityBrokenException() {
        super();
    }

    public EntityBrokenException(String message) {
        super(message);
    }

    public EntityBrokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntityBrokenException(Throwable cause) {
        super(cause);
    }

    protected EntityBrokenException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
