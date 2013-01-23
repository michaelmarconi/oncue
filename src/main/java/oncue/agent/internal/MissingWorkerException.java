package oncue.agent.internal;

public class MissingWorkerException extends Exception {

	private static final long serialVersionUID = -5306846667479086348L;

	public MissingWorkerException(String message, Throwable cause) {
		super(message, cause);
	}
}
