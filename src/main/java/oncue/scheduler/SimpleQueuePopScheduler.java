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

import oncue.backingstore.internal.BackingStore;
import oncue.messages.internal.AbstractWorkRequest;
import oncue.scheduler.internal.AbstractScheduler;
import oncue.scheduler.internal.NoJobsException;
import oncue.scheduler.internal.Schedule;

/**
 * This concrete implementation of {@linkplain AbstractScheduler} employs a very
 * simple scheduling strategy that pops the next job off the queue and hands it
 * to the first requesting agent.
 * 
 * This is strictly first-come-first-served: which ever agent makes the request
 * first will get the job.
 */
public class SimpleQueuePopScheduler extends AbstractScheduler {

	public SimpleQueuePopScheduler(Class<? extends BackingStore> backingStore) {
		super(backingStore);
	}

	@Override
	protected void scheduleJobs(AbstractWorkRequest workRequest) {

		// Create a schedule
		Schedule schedule = new Schedule();
		try {
			schedule.setJob(getSender(), unscheduledJobs.popJob());
		} catch (NoJobsException e) {
			// Ignore; no jobs on the queue
		}

		// Dispatch the schedule
		dispatchJobs(schedule);
	}

}
