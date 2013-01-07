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

import oncue.backingstore.internal.IBackingStore;
import oncue.messages.ThrottledWorkRequest;
import oncue.messages.internal.AbstractWorkRequest;
import oncue.messages.internal.Job;
import oncue.scheduler.internal.AbstractScheduler;
import oncue.scheduler.internal.NoJobsException;
import oncue.scheduler.internal.Schedule;

public class ThrottledScheduler extends AbstractScheduler {

	public ThrottledScheduler(Class<? extends IBackingStore> backingStore) {
		super(backingStore);
	}

	@Override
	protected void scheduleJobs(AbstractWorkRequest workRequest) {
		ThrottledWorkRequest throttledWorkRequest = (ThrottledWorkRequest) workRequest;

		// Pop the requested number of jobs
		List<Job> jobs = new ArrayList<>();
		for (int i = 0; i < throttledWorkRequest.getJobs(); i++) {
			try {
				jobs.add(unscheduledJobs.popJob());
			} catch (NoJobsException e) {
				// No more jobs on the queue!
				break;
			}
		}

		// Create a schedule
		Schedule schedule = new Schedule();
		schedule.setJobs(getSender(), jobs);

		// Dispatch the schedule
		dispatchJobs(schedule);
	}

}
