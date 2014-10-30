/*******************************************************************************
 * Copyright 2013 Michael Marconi
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package oncue.scheduler;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import oncue.backingstore.BackingStore;
import oncue.common.messages.Job;
import akka.event.LoggingAdapter;

/**
 * An encapsulated job queue of unscheduled {@linkplain Job}s that relies on a backing store for
 * persistence.
 */
public class UnscheduledJobs {

	// The persistent backing store
	private final BackingStore backingStore;

	private final LoggingAdapter log;

	// The prioritised queue of unscheduled jobs
	private final SortedSet<Job> unscheduledJobs;

	/**
	 * @param backingStore is an instance of {@linkplain BackingStore}
	 */
	public UnscheduledJobs(BackingStore backingStore, LoggingAdapter log, Comparator<Job> jobComparator) {
		this.unscheduledJobs = new TreeSet<>(jobComparator);
		this.backingStore = backingStore;
		this.log = log;
		restoreJobs();
	}

	/**
	 * Add a job to the queue
	 */
	public void addJob(Job job) {
		backingStore.addUnscheduledJob(job);
		unscheduledJobs.add(job);
	}

	/**
	 * @return the number of jobs in the queue
	 */
	public int getSize() {
		return unscheduledJobs.size();
	}

	/**
	 * @return the set of worker types that enqueued jobs require to process
	 */
	public Set<String> getWorkerTypes() {
		Set<String> workerTypes = new HashSet<>();
		Iterator<Job> itrJobs = unscheduledJobs.iterator();
		while (itrJobs.hasNext()) {
			Job job = itrJobs.next();
			workerTypes.add(job.getWorkerType());
		}
		return workerTypes;
	}

	/**
	 * @return true if there are no more unscheduled jobs
	 */
	public boolean isEmpty() {
		return unscheduledJobs.isEmpty();
	}

	/**
	 * Determine if there are unscheduled jobs for the specified worker type.
	 */
	public boolean isWorkAvailable(Set<String> workerTypes) {
		Iterator<Job> itrJobs = unscheduledJobs.iterator();
		while (itrJobs.hasNext()) {
			Job job = itrJobs.next();
			if (workerTypes.contains(job.getWorkerType()))
				return true;
		}
		return false;
	}

	/**
	 * @return a thread-safe iterator over the unscheduled jobs.
	 */
	public Iterator<Job> iterator() {
		return unscheduledJobs.iterator();
	}

	/**
	 * Remove a job from anywhere in the queue
	 * 
	 * @return a boolean, indicating if the removal was successful
	 */
	public boolean removeJob(Job job) {
		boolean removed = unscheduledJobs.remove(job);
		if (removed)
			backingStore.removeUnscheduledJob(job);

		return removed;
	}

	/**
	 * Remove a list of jobs from anywhere in the queue
	 * 
	 * @return a boolean, indicating if the removal was successful
	 */
	public boolean removeJobs(List<Job> jobs) {
		boolean removed = unscheduledJobs.removeAll(jobs);
		if (backingStore != null && removed)
			for (Job job : jobs) {
				backingStore.removeUnscheduledJob(job);
			}
		return removed;
	}

	/**
	 * Restore any scheduled and unscheduled jobs from the backing store
	 */
	private void restoreJobs() {
		List<Job> restoredJobs = backingStore.restoreJobs();
		if (restoredJobs != null && !restoredJobs.isEmpty())
			log.info("Restoring {} jobs from the backing store", restoredJobs.size());

		unscheduledJobs.addAll(restoredJobs);
	}

}
