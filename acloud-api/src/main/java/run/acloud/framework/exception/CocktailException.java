package run.acloud.framework.exception;

import lombok.Getter;
import lombok.Setter;
import run.acloud.framework.enums.ExceptionBiz;
import run.acloud.framework.enums.ExceptionType;

public class CocktailException extends RuntimeException {
	private static final long serialVersionUID = 8551277964333695972L;

	@Getter
    private ExceptionType type = ExceptionType.InternalError;
	
	@Getter
	@Setter
	private Object data;

	@Getter
	private Integer httpStatusCode;

	@Getter
	private ExceptionBiz biz;

    @Getter
    private String additionalMessage;

    public CocktailException(Throwable cause, ExceptionType type, Object data, ExceptionBiz biz) {
        super(cause);
        this.type = type;
        this.data = data;
        this.biz = biz;
        this.additionalMessage = cause.getMessage();
    }

    public CocktailException(Throwable cause, ExceptionType type, Object data) {
        super(cause);
        this.type = type;
        this.data = data;
        this.additionalMessage = cause.getMessage();
    }

    public CocktailException(String message) {
        super(message);
        this.additionalMessage = message;
    }

    public CocktailException(String message, ExceptionType type, ExceptionBiz biz) {
        super(message);
        this.type = type;
        this.biz = biz;
        this.additionalMessage = message;
    }

    public CocktailException(String message, ExceptionType type, ExceptionBiz biz, String additionalMessage) {
        super(message);
        this.type = type;
        this.biz = biz;
        this.additionalMessage = additionalMessage;
    }

    public CocktailException(String message, ExceptionType type) {
        super(message);
        this.type = type;
        this.additionalMessage = message;
    }

    public CocktailException(String message, ExceptionType type, Object data, ExceptionBiz biz) {
        super(message);
        this.type = type;
        this.data = data;
        this.biz = biz;
        this.additionalMessage = message;
    }

    public CocktailException(String message, ExceptionType type, Object data) {
        super(message);
        this.type = type;
        this.data = data;
        this.additionalMessage = message;
    }

    public CocktailException(String message, ExceptionType type, String additionalMessage) {
        super(message);
        this.type = type;
        this.additionalMessage = additionalMessage;
    }

    public CocktailException(String message, Throwable cause, ExceptionType type, Object data, ExceptionBiz biz) {
        super(message, cause);
        this.type = type;
        this.data = data;
        this.biz = biz;
        this.additionalMessage = message;
    }

    public CocktailException(String message, Throwable cause, ExceptionType type, Object data) {
        super(message, cause);
        this.type = type;
        this.data = data;
        this.additionalMessage = message;
    }

    public CocktailException(String message, ExceptionType type, Integer httpStatusCode) {
        super(message);
        this.type = type;
        this.httpStatusCode = httpStatusCode;
        this.additionalMessage = message;
    }

    public CocktailException(String message, Throwable cause, ExceptionType type, Object data, Integer httpStatusCode) {
        super(message, cause);
        this.type = type;
        this.data = data;
        this.httpStatusCode = httpStatusCode;
        this.additionalMessage = message;
    }

    public CocktailException(String message, Throwable cause, ExceptionType type, String additionalMessage) {
        super(message, cause);
        this.type = type;
        this.additionalMessage = additionalMessage;
    }

    public CocktailException(String message, Throwable cause, ExceptionType type, String additionalMessage, Integer httpStatusCode) {
        super(message, cause);
        this.type = type;
        this.additionalMessage = additionalMessage;
        this.httpStatusCode = httpStatusCode;
    }

    public CocktailException(String message, Throwable cause, ExceptionType type, ExceptionBiz biz) {
        super(message, cause);
        this.type = type;
        this.biz = biz;
        this.additionalMessage = message;
    }

    public CocktailException(String message, String additionalMessage, Throwable cause, ExceptionType type, ExceptionBiz biz) {
        super(message, cause);
        this.additionalMessage = additionalMessage;
        this.type = type;
        this.biz = biz;
    }

    public CocktailException(String message, Throwable cause, ExceptionType type) {
        super(message, cause);
        this.type = type;
        this.additionalMessage = message;
    }

}
