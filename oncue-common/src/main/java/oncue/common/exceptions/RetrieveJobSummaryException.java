package oncue.common.exceptions;

public class RetrieveJobSummaryException extends Exception {

	private static final long serialVersionUID = -4083107548878832578L;

	public RetrieveJobSummaryException(String message) {
		super(message);
	}

	public RetrieveJobSummaryException() {
		super();
	}

	public RetrieveJobSummaryException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public RetrieveJobSummaryException(String message, Throwable cause) {
		super(message, cause);
	}

	public RetrieveJobSummaryException(Throwable cause) {
		super(cause);
	}

}
