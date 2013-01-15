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
package oncue.backingstore.internal;

import java.util.List;

import oncue.messages.internal.Job;
import oncue.messages.internal.JobFailed;
import oncue.messages.internal.JobProgress;

/**
 * The contract for all persistent data stores.
 */
public interface BackingStore {

	/**
	 * Persist the jobs to the list of scheduled jobs
	 * 
	 * @param scheduledJobs
	 *            is the list of {@linkplain Job}s assigned to the agent
	 */
	public void addScheduledJobs(List<Job> scheduledJobs);

	/**
	 * Add a job to the unscheduled jobs queue
	 */
	public void addUnscheduledJob(Job job);

	/**
	 * Persist the details of a failed job
	 * 
	 * @param jobFailed
	 *            contains the details of the job that failed and the associated
	 *            failure
	 */
	public void persistJobFailure(JobFailed jobFailed);

	/**
	 * Persist the progress made against a job
	 * 
	 * @param jobProgress
	 *            is a record of the progress a worker has made against this
	 *            job.
	 */
	public void persistJobProgress(JobProgress jobProgress);

	/**
	 * Pop the first job off the unscheduled jobs queue
	 * 
	 * @return the id of the popped job
	 */
	public long popUnscheduledJob();

	/**
	 * Remove a job from the list of scheduled jobs
	 * 
	 * @param job
	 *            is the {@linkplain Job} to remove
	 */
	public void removeScheduledJob(Job job);

	/**
	 * Remove a job from the unscheduled jobs queue
	 */
	public void removeUnscheduledJob(Job job);

	/**
	 * Restore the unscheduled jobs queue from both scheduled and unscheduled
	 * jobs, as we assume that all agents are dead.
	 * 
	 * @return a list of {@linkplain Job}
	 */
	public List<Job> restoreJobs();

}
