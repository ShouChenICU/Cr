package icu.mmmc.cr.exceptions;

/**
 * 身份异常
 *
 * @author shouchen
 */
@SuppressWarnings("unused")
public class IdentityException extends Exception {
    public IdentityException() {
    }

    public IdentityException(String message) {
        super(message);
    }

    public IdentityException(String message, Throwable cause) {
        super(message, cause);
    }

    public IdentityException(Throwable cause) {
        super(cause);
    }

    public IdentityException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
