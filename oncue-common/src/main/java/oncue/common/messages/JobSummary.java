package oncue.common.messages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Joiner;

/**
 * A scheduler will send this message in response to a simple job summary
 * request.
 */
public class JobSummary implements Serializable {

	private static final long serialVersionUID = 8252819036997216081L;

	private List<Job> jobs = new ArrayList<>();

	/**
	 * empty constructor required for JSON mapping
	 */
	public JobSummary() {
	}

	public JobSummary(Collection<Job> jobs) {
		super();
		for (Job job : jobs) {
			this.jobs.add((Job) job.clone());
		}
	}

	public List<Job> getJobs() {
		return jobs;
	}

	@Override
	public String toString() {
		return Joiner.on('|').join(jobs).toString();
	}
}
