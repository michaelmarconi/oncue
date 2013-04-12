package oncue.common.messages;

import java.io.Serializable;
import java.util.List;

/**
 * A scheduler will send this message in response to a simple job summary
 * request.
 */
public class JobSummary implements Serializable {

	private static final long serialVersionUID = 8252819036997216081L;

	private List<Job> jobs;

	/**
	 * empty constructor required for JSON mapping
	 */
	public JobSummary() {
	}

	public JobSummary(List<Job> jobs) {
		super();
		this.jobs = jobs;
	}

	public List<Job> getJobs() {
		return jobs;
	}
}
