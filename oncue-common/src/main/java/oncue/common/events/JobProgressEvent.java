package oncue.common.events;

import java.io.Serializable;

import oncue.common.messages.Job;

/**
 * This event is fired when progress is made on a job.
 */
public class JobProgressEvent implements Serializable {

	private static final long serialVersionUID = 7779509571217733670L;
	private Job job;

	public JobProgressEvent(Job job) {
		super();
		this.setJob(job);
	}

	public Job getJob() {
		return job;
	}

	public void setJob(Job job) {
		this.job = job;
	}

}
