/*******************************************************************************
 * Copyright 2013 Michael Marconi
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package oncue.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import oncue.backingstore.BackingStore;
import oncue.common.messages.Agent;
import oncue.common.messages.Job;
import oncue.scheduler.exceptions.RemoveScheduleJobException;

/**
 * An encapsulated map of {@linkplain Job}s to the {@linkplain Agent}s they have
 * been scheduled against.
 */
public class ScheduledJobs {

	// A persistent backing store
	private BackingStore backingStore;

	// Map a list of scheduled jobs to logical agent address
	private Map<String, List<Job>> scheduledJobs = new ConcurrentHashMap<String, List<Job>>();

	/**
	 * @param backingStore
	 *            is an instance of {@linkplain BackingStore}
	 */
	public ScheduledJobs(BackingStore backingStore) {
		this.backingStore = backingStore;
	}

	/**
	 * Assign a list of jobs to an agent
	 * 
	 * @param agent
	 *            is the {@linkplain Agent} to which the jobs are being assigned
	 * @param jobs
	 *            is the list of {@linkplain Jobs}s to assign to the agent
	 */
	public void addJobs(String agent, List<Job> jobs) {

		List<Job> assignedJobs = scheduledJobs.get(agent);
		if (assignedJobs == null) {
			assignedJobs = new ArrayList<Job>();
			scheduledJobs.put(agent, assignedJobs);
		}
		assignedJobs.addAll(jobs);
		backingStore.addScheduledJobs(jobs);
	}

	/**
	 * @return a list of scheduled jobs
	 */
	public List<Job> getJobs() {
		List<Job> jobs = new ArrayList<>();
		for (String agent : scheduledJobs.keySet()) {
			jobs.addAll(scheduledJobs.get(agent));
		}
		return jobs;

	}

	/**
	 * Get the list of jobs associated with this agent
	 * 
	 * @param agent
	 *            is the {@linkplain Agent} the jobs are associated with
	 * @return a list of {@linkplain Job}s associated with the agent
	 */
	public List<Job> getJobs(String agent) {
		List<Job> jobs = scheduledJobs.get(agent);
		if (jobs == null)
			return new ArrayList<>();
		else
			return jobs;
	}

	/**
	 * Update the state and progress of a scheduled job, usually in response to
	 * work done on the job.
	 * 
	 * @param job
	 *            contains the updates
	 * @param agent
	 *            is where the work is being done
	 */
	public void updateJob(Job job, String agent) {
		List<Job> jobs = getJobs(agent);
		for (Job scheduledJob : jobs) {
			if (scheduledJob.getId() == job.getId()) {
				scheduledJob.setProgress(job.getProgress());
				scheduledJob.setState(job.getState());
			}
		}
	}

	/**
	 * Remove a job associated with an agent
	 * 
	 * @param job
	 *            is the {@linkplain Job} to remove
	 * 
	 * @throws RemoveScheduleJobException
	 */
	public void removeJob(Job job, String agent)
			throws RemoveScheduleJobException {

		if (!scheduledJobs.containsKey(agent))
			throw new RemoveScheduleJobException(
					"There is no registered agent "
							+ agent
							+ ".  It is possible the scheduler was restarted and this agent has not re-registered yet.");

		Job jobToRemove = null;
		for (Job scheduledJob : scheduledJobs.get(agent)) {
			if (scheduledJob.getId() == job.getId())
				jobToRemove = scheduledJob;
		}

		scheduledJobs.get(agent).remove(jobToRemove);
		backingStore.removeScheduledJob(job);
	}

	public List<Job> getScheduledJobs() {
		List<Job> response = new ArrayList<>();
		for(List<Job> jobs : scheduledJobs.values()) {
			response.addAll(jobs);
		}
		return response;
	}
}
