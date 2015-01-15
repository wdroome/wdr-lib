package com.wdroome.json;

/**
 * Base class for all JSON Exceptions.
 * @author wdr
 */
public class JSONException extends Exception
{
	private static final long serialVersionUID = 0;
	private Throwable cause;

    /**
     * Constructs a JSONException with an explanatory message.
     * @param message Detail about the reason for the exception.
     */
    public JSONException(String message) {
        super(message);
    }

    public JSONException(Throwable cause) {
        super(cause.getMessage());
        this.cause = cause;
    }

    public Throwable getCause() {
        return this.cause;
    }
}
