package oncue.timedJobs;

public class TimedJobException extends Exception {

	private static final long serialVersionUID = 8165869785630376062L;

	public TimedJobException(String message) {
		super(message);
	}

	public TimedJobException(String message, Throwable cause) {
		super(message, cause);
	}
}
