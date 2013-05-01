package oncue.common.events;

import java.io.Serializable;

import oncue.common.messages.Job;

/**
 * This event is fired when a job is enqueued.
 */
public class JobEnqueuedEvent implements Serializable {

	private static final long serialVersionUID = 7820441419463038812L;

	private Job job;

	public JobEnqueuedEvent(Job job) {
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
