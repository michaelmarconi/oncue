package oncue.backingstore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import oncue.common.messages.Job;
import oncue.common.settings.Settings;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import akka.actor.ActorSystem;

/**
 * A simple in-memory backing store, which is not persistent across restarts.
 * Use this for testing only!
 */
public class InMemoryBackingStore extends AbstractBackingStore {

	private List<Job> scheduledJobs = new ArrayList<>();
	private List<Job> unscheduledJobs = new ArrayList<>();
	private List<Job> completedJobs = new ArrayList<>();
	private List<Job> failedJobs = new ArrayList<>();
	private long nextJobID = 1;

	public InMemoryBackingStore(ActorSystem system, Settings settings) {
		super(system, settings);
	}

	@Override
	public void addScheduledJobs(List<Job> scheduledJobs) {
		this.scheduledJobs.addAll(scheduledJobs);
	}

	@Override
	public void addUnscheduledJob(Job job) {
		this.unscheduledJobs.add(job);
	}

	@Override
	public List<Job> getCompletedJobs() {
		return completedJobs;
	}

	@Override
	public List<Job> getFailedJobs() {
		return failedJobs;
	}

	@Override
	public long getNextJobID() {
		return this.nextJobID++;
	}

	@Override
	public void persistJobFailure(Job job) {
		failedJobs.add(job);
	}

	@Override
	public void persistJobProgress(Job job) {
		for (Job scheduledJob : scheduledJobs) {
			if (scheduledJob.getId() == job.getId()) {
				scheduledJob.setProgress(job.getProgress());
				if (scheduledJob.getState() == Job.State.COMPLETE) {
					completedJobs.add(scheduledJob);
				}
				break;
			}
		}
	}

	@Override
	public void removeScheduledJob(Job job) {
		scheduledJobs.remove(job);
	}

	@Override
	public void removeUnscheduledJob(Job job) {
		unscheduledJobs.remove(job);
	}

	@Override
	public List<Job> restoreJobs() {
		return Collections.emptyList();
	}

	@Override
	public void removeCompletedJob(Job job) {
		completedJobs.remove(job);
	}

	@Override
	public void removeFailedJob(Job job) {
		failedJobs.remove(job);
	}

	@Override
	public void cleanupJobs(boolean includeFailedJobs, Duration expirationAge) {
		List<Job> expiredCompletedJobs = new ArrayList<>();
		for (Job completedJob : completedJobs) {
			DateTime expirationThreshold = DateTime.now().minus(expirationAge.getMillis());
			boolean isExpired = completedJob.getCompletedAt().isBefore(expirationThreshold.toInstant());
			if (isExpired) {
				expiredCompletedJobs.add(completedJob);
			}
		}
		completedJobs.removeAll(expiredCompletedJobs);
		
		if (!includeFailedJobs) {
			return;
		}

		List<Job> expiredFailedJobs = new ArrayList<>();
		for (Job failedJob : failedJobs) {
			DateTime expirationThreshold = DateTime.now().minus(expirationAge.getMillis());
			boolean isExpired = failedJob.getCompletedAt().isBefore(expirationThreshold.toInstant());
			if (isExpired) {
				expiredFailedJobs.add(failedJob);
			}
		}
		expiredFailedJobs.removeAll(expiredFailedJobs);
	}
}
