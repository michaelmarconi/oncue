package oncue.common.messages;

import java.io.Serializable;
import java.util.List;

/**
 * A scheduler will send this message in response to a simple job summary
 * request.
 */
public class JobSummaryResponse implements Serializable {

	private static final long serialVersionUID = 8252819036997216081L;

	private List<Job> jobs;

	public List<Job> getJobs() {
		return jobs;
	}

	public JobSummaryResponse(List<Job> jobs) {
		super();
		this.jobs = jobs;
	}
}
