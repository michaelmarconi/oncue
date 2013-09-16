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
package oncue.backingstore;

import java.util.List;

import oncue.common.messages.Job;

import org.joda.time.Duration;

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
	 * Clean up complete and optionally, failed jobs from the backing store
	 * 
	 * @param includeFailedJobs
	 *            determines whether failed jobs will also be cleaned up
	 * @param expirationAge
	 *            is the duration that must have elapsed before the job is
	 *            eligible for cleanup, e.g. "24 hours" means that the job won't
	 *            be cleaned up until at least 24 hours have elapsed since it
	 *            completed.
	 */
	public void cleanupJobs(boolean includeFailedJobs, Duration expirationAge);

	/**
	 * The backing store makes a note of jobs that complete successfully. Get
	 * the list of these.
	 * 
	 * @return a list of complete {@linkplain Job}
	 */
	public List<Job> getCompletedJobs();

	/**
	 * The backing store makes a note of jobs that fail. Get the list of these.
	 * 
	 * @return a list of failed {@linkplain Job}
	 */
	public List<Job> getFailedJobs();

	/**
	 * Fetch the next monotonically-increasing job identifier. NOTE: This action
	 * *must* increment the job identifier in the persistent store, as well as
	 * return it!
	 */
	public long getNextJobID();

	/**
	 * Persist the details of a failed job
	 * 
	 * @param job
	 *            is the job that has failed
	 */
	public void persistJobFailure(Job job);

	/**
	 * Persist the progress made against a job
	 * 
	 * @param job
	 *            contains a record of progress made by a worker
	 */
	public void persistJobProgress(Job job);

	/**
	 * Remove a job from the list of completed jobs
	 */
	public void removeCompletedJob(Job job);

	/**
	 * Remove a job from the list of failed jobs
	 */
	public void removeFailedJob(Job job);

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
