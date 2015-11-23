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
import java.util.Iterator;
import java.util.List;

import oncue.backingstore.BackingStore;
import oncue.common.messages.Job;
import oncue.common.messages.ThrottledWorkRequest;

/**
 * This implementation of {@linkplain AbstractScheduler} employs a throttling
 * strategy to ensure that agents are never overwhelmed with work.
 * 
 * A <code>oncue.agent.ThrottledAgent</code> will send a {@linkplain ThrottledWorkRequest},
 * stating the number of jobs it is able to process in parallel. This scheduler
 * will pop just enough jobs off the queue to satisfy this throttled request for
 * work.
 */
public class ThrottledScheduler extends AbstractScheduler<ThrottledWorkRequest> {

	public ThrottledScheduler(Class<? extends BackingStore> backingStore) {
		super(backingStore);
	}

	@Override
	protected void scheduleJobs(ThrottledWorkRequest workRequest) {

		List<Job> jobs = new ArrayList<>();
		Iterator<Job> iterator = unscheduledJobs.iterator();
		while (iterator.hasNext() && jobs.size() < workRequest.getMaxJobs()) {
			Job job = iterator.next();
			if (workRequest.getWorkerTypes().contains(job.getWorkerType()))
				jobs.add(job);
		}

		// Create the schedule
		Schedule schedule = new Schedule();
		schedule.setJobs(getSender(), jobs);

		// Dispatch the schedule
		dispatchJobs(schedule);
	}

}
