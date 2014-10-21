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
import oncue.common.messages.UnmodifiableJob;

import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * TODO
 */
public class CubeCapacityScheduler extends AbstractScheduler<CubeCapacityWorkRequest> {

	public CubeCapacityScheduler(Class<? extends BackingStore> backingStore) {
		super(backingStore);
	}

	protected Comparator<Job> getComparator() {
		return new PriorityOrderedStableEnqueueDateJobComparator();
	}

	private boolean isRequiredWorkerType(UnmodifiableJob job) {
		Config config = getContext().system().settings().config();
//		System.err.println(config.root().render());

		return job.getWorkerType().equals(
				config.getConfig("oncue.scheduler.cube_capacity_scheduler")
						.getString("worker_type"));
//		return job.getWorkerType().equals("com.modeltwozero.universal.oncue.workers.MatchingWorker");
	}

	@Override
	protected void scheduleJobs(CubeCapacityWorkRequest workRequest) {
//		System.err.println(ConfigFactory.load().root().render());
		Set<String> runningMatchingJobs = getRunningMatchingJobs();
		List<Job> jobs = new ArrayList<>();
		int allocatedMemory = 0;

		Iterator<Job> iterator = unscheduledJobs.iterator();

		while (iterator.hasNext()) {
			Job job = iterator.next();
			if (workRequest.getWorkerTypes().contains(job.getWorkerType())) {
				Map<String, String> params = job.getParams();
				int requiredMemory = 0;
				if(params != null && params.get("memory") != null) { 
					requiredMemory = Integer.parseInt(params.get("memory"));
				}
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
		// TODO: Log this

		// Create the schedule
		Schedule schedule = new Schedule();
		schedule.setJobs(getSender(), jobs);

		// Dispatch the schedule
		dispatchJobs(schedule);
	}

	private Set<String> getRunningMatchingJobs() {
		List<UnmodifiableJob> scheduledJobs = getScheduledJobs();
		Set<String> runningMatchingJobs = Sets.newHashSet();
		for (UnmodifiableJob job : scheduledJobs) {
			if (isRequiredWorkerType(job)) {
				runningMatchingJobs.add(job.getParams().get("process_code"));
			}
		}
		return runningMatchingJobs;
	}

}
