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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import oncue.backingstore.BackingStore;
import oncue.common.messages.CubeCapacityWorkRequest;
import oncue.common.messages.Job;

import com.google.common.collect.Sets;
import com.typesafe.config.Config;

/**
 * A barebones Cube scheduler.
 * 
 * Hands out jobs in priority order. This assumes a Job has a parameter value of "priority". If a
 * job does not provide a priority, it is assumed to be 0 (low priority).
 * 
 * This scheduler will only provide
 * 
 */
public class CubeCapacityScheduler extends AbstractScheduler<CubeCapacityWorkRequest> {

	public CubeCapacityScheduler(Class<? extends BackingStore> backingStore) {
		super(backingStore);
	}

	protected Comparator<Job> getComparator() {
		return new PriorityOrderedStableEnqueueDateJobComparator();
	}

	private boolean isRequiredWorkerType(Job job) {
		Config config = getContext().system().settings().config();
		return job.getWorkerType().equals(
				config.getConfig("oncue.scheduler.cube_capacity_scheduler").getString(
						"matching_worker_type"));
	}

	@Override
	protected void scheduleJobs(CubeCapacityWorkRequest workRequest) {
		Set<String> runningMatchingJobs = getScheduledMatchingJobCodes();
		List<Job> jobs = new ArrayList<>();
		int allocatedMemory = 0;

		Iterator<Job> iterator = unscheduledJobs.iterator();

		while (iterator.hasNext()) {
			Job job = iterator.next();
			if (workRequest.getWorkerTypes().contains(job.getWorkerType())) {
				Map<String, String> params = job.getParams();
				int requiredMemory = getRequiredMemory(job);
				if (requiredMemory + allocatedMemory <= workRequest.getAvailableMemory()) {
					if (isRequiredWorkerType(job)) {
						String processCode = params.get("process_code");
						if (runningMatchingJobs.contains(processCode)) {
							continue;
						} else {
							runningMatchingJobs.add(processCode);
						}
					}

					jobs.add(job);
					allocatedMemory += requiredMemory;
				}
			}
		}

		log.debug("Scheduling {} job(s) with a total memory of {}", jobs.size(), allocatedMemory);

		// Create the schedule
		Schedule schedule = new Schedule();
		schedule.setJobs(getSender(), jobs);

		// Dispatch the schedule
		dispatchJobs(schedule);
	}

	@Override
	protected void augmentJob(Job job) {
		ensureRequiredMemory(job);
	}

	private void ensureRequiredMemory(Job job) {
		Map<String, String> params = job.getParams();
		if (!params.containsKey("memory")) {
			Config config = getContext().system().settings().config();
			params.put("memory", String.valueOf(config.getConfig(
					"oncue.scheduler.cube_capacity_scheduler").getInt(
					"default_requirements." + job.getWorkerType() + ".memory")));
		}
	}

	private int getRequiredMemory(Job job) {
		return Integer.parseInt(job.getParams().get("memory"));
	}

	private Set<String> getScheduledMatchingJobCodes() {
		List<Job> scheduledJobs = getScheduledJobs();
		Set<String> scheduledMatchingJobCodes = Sets.newHashSet();
		for (Job job : scheduledJobs) {
			if (isRequiredWorkerType(job)) {
				scheduledMatchingJobCodes.add(job.getParams().get("process_code"));
			}
		}
		return scheduledMatchingJobCodes;
	}

}