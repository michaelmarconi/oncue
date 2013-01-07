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
package oncue.scheduler.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oncue.backingstore.internal.IBackingStore;
import oncue.messages.internal.Job;
import sun.management.Agent;
import akka.actor.ActorRef;

/**
 * An encapsulated map of {@linkplain Job}s to the {@linkplain Agent}s they have
 * been scheduled against.
 */
public class ScheduledJobs {

	// Map a list of scheduled jobs to logical agent address
	private Map<String, List<Job>> scheduledJobs = new HashMap<String, List<Job>>();

	// An optional, persistent backing store
	private IBackingStore backingStore;

	/**
	 * @param backingStore
	 *            is an optional instance of {@linkplain IBackingStore}
	 */
	public ScheduledJobs(IBackingStore backingStore) {
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
	public void addJobs(ActorRef agent, List<Job> jobs) {

		List<Job> assignedJobs = scheduledJobs.get(agent);
		if (assignedJobs == null) {
			assignedJobs = new ArrayList<Job>();
			scheduledJobs.put(agent.path().toString(), assignedJobs);
		}
		assignedJobs.addAll(jobs);

		if (backingStore != null)
			backingStore.addScheduledJobs(jobs);
	}

	/**
	 * Get the list of jobs associated with this agent
	 * 
	 * @param agent
	 *            is the {@linkplain Agent} the jobs are associated with
	 * @return a list of {@linkplain Job}s associated with the agent
	 */
	public List<Job> getJobs(ActorRef agent) {
		List<Job> jobs = scheduledJobs.get(agent.path().toString());
		if (jobs == null)
			return new ArrayList<>();
		else
			return jobs;
	}

	/**
	 * Remove a job associated with an agent
	 * 
	 * @param job
	 *            is the {@linkplain Job} to remove
	 */
	public void removeJob(Job job, ActorRef agent) {
		scheduledJobs.get(agent.path().toString()).remove(job);

		if (backingStore != null)
			backingStore.removeScheduledJob(job);
	}
}
