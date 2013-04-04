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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oncue.backingstore.BackingStore;
import oncue.common.messages.JVMCapacityWorkRequest;
import oncue.common.messages.Job;
import oncue.common.messages.SimpleMessages.SimpleMessage;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.Cancellable;

/**
 * A JVM capacity-aware scheduler will collect work requests for the specified
 * amount of time (giving a collection of agents time to respond to a work
 * broadcast) and then schedule jobs according to the memory capacity reported
 * by each agent.
 */
public class JVMCapacityScheduler extends AbstractScheduler<JVMCapacityWorkRequest> {

	// The list of work requests in this time window
	List<JVMCapacityWorkRequest> workRequests = new ArrayList<>();

	// A scheduled request to schedule jobs
	private Cancellable scheduleJobs;

	// The size parameter on the job
	public static final String JOB_SIZE = "size";

	public JVMCapacityScheduler(Class<? extends BackingStore> backingStore) {
		super(backingStore);
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message.equals(SimpleMessage.SCHEDULE_JOBS)) {
			scheduleJobs();
		} else
			super.onReceive(message);
	}

	/**
	 * <p>
	 * The real brains of this scheduler: using the memory capacity reported in
	 * each work request, determine how to spread the load of unscheduled jobs
	 * efficiently across the requesting agents.
	 * </p>
	 * 
	 * <p>
	 * The algorithm works by taking the collection of work requests and sorting
	 * them from smallest to largest free memory capacity.
	 * 
	 * The scheduler then iterates over each job, handing it to the first agent
	 * that has enough capacity to complete it. In this sense, each agent is
	 * "greedy", grabbing as many jobs as it can manage.
	 * 
	 * If a job is too big to fit into the remaining capacity of any of the
	 * agents, it will remain unscheduled.
	 * </p>
	 * 
	 * <p>
	 * <b>WARNING:</b> If a job is too big for any of the agents, it will sit on
	 * the backlog forever!
	 * <p>
	 */
	private void scheduleJobs() {

		Map<ActorRef, List<Job>> agentJobs = new HashMap<ActorRef, List<Job>>();
		List<Job> scheduledJobs = new ArrayList<>();

		for (Job job : unscheduledJobs.getJobs()) {
			long jobSize = new Long(job.getParams().get(JVMCapacityScheduler.JOB_SIZE));
			sortWorkRequestsByFreeMemory();

			for (JVMCapacityWorkRequest workRequest : workRequests) {
				long freeMemory = workRequest.getFreeMemory();
				ActorRef agent = workRequest.getAgent();
				if (freeMemory >= jobSize) {
					// Add the job to the agent
					List<Job> jobs = agentJobs.get(agent);
					if (jobs == null) {
						jobs = new ArrayList<Job>();
						agentJobs.put(agent, jobs);
					}
					jobs.add(job);
					scheduledJobs.add(job);

					// Decrease agent's available memory
					workRequest.setFreeMemory(freeMemory - jobSize);
					break;
				}
			}
		}

		// Remove the jobs from the scheduled queue
		for (Job job : scheduledJobs) {
			unscheduledJobs.removeJob(job);
		}

		// Create a schedule
		Schedule schedule = new Schedule();
		for (ActorRef agent : agentJobs.keySet()) {
			schedule.setJobs(agent, agentJobs.get(agent));
		}

		// Dispatch the schedule
		dispatchJobs(schedule);

		// Clear old work requests
		workRequests.clear();

		// Cancel the scheduling
		scheduleJobs.cancel();
	}

	@Override
	protected void scheduleJobs(JVMCapacityWorkRequest workRequest) {

		// Add to the map of unserviced work requests
		workRequests.add(workRequest);

		/*
		 * Give agents the specified amount of time to react to a jobs broadcast
		 * by scheduling a request to create a job schedule in the future
		 */
		// TODO move duration into config
		if (scheduleJobs == null || scheduleJobs.isCancelled()) {
			scheduleJobs = getContext().system().scheduler()
					.scheduleOnce((FiniteDuration) Duration.create("1 second"), new Runnable() {

						@Override
						public void run() {
							getSelf().tell(SimpleMessage.SCHEDULE_JOBS, getSelf());
						}
					}, getContext().dispatcher());
		}
	}

	private void sortWorkRequestsByFreeMemory() {
		Collections.sort(workRequests, new Comparator<JVMCapacityWorkRequest>() {

			@Override
			public int compare(JVMCapacityWorkRequest workRequest1, JVMCapacityWorkRequest workRequest2) {
				return new Long(workRequest1.getFreeMemory()).compareTo(new Long(workRequest2.getFreeMemory()));
			}
		});
	}

}
