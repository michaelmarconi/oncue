package oncue.scheduler.exceptions;

public class JobNotFoundException extends Exception {

	private static final long serialVersionUID = 8730069910277419757L;

	public JobNotFoundException() {
	}

	public JobNotFoundException(String message) {
		super(message);
	}

	public JobNotFoundException(Throwable cause) {
		super(cause);
	}

	public JobNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public JobNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
