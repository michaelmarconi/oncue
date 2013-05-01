package oncue.common.events;

import java.io.Serializable;

import oncue.common.messages.Job;

/**
 * This event is fired when progress is made on a job.
 */
public class JobFailedEvent implements Serializable {

	private static final long serialVersionUID = 604402719393058553L;
	private Job job;

	public JobFailedEvent(Job job) {
		super();
		this.job = job;
	}

	public Job getJob() {
		return job;
	}

}
