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

import com.sun.org.glassfish.external.statistics.impl.AverageRangeStatisticImpl;

import oncue.backingstore.BackingStore;
import oncue.common.messages.CubeCapacityWorkRequest;
import oncue.common.messages.Job;

/**
 * TODO
 */
public class CubeCapacityScheduler extends AbstractScheduler<CubeCapacityWorkRequest> {

	public CubeCapacityScheduler(Class<? extends BackingStore> backingStore) {
		super(backingStore);
	}

	@Override
	protected void scheduleJobs(CubeCapacityWorkRequest workRequest) {

		List<Job> jobs = new ArrayList<>();
		Iterator<Job> iterator = unscheduledJobs.iterator();
		int allocatedMemory = 0;
//		= workRequest.getAvailableMemory();
//		while (iterator.hasNext()) {
//			Job job = iterator.next();
//			if (workRequest.getWorkerTypes().contains(job.getWorkerType())) {
//				int requiredMemory = new Integer(job.getParams().get("memory"));
//				if requiredMemory <= allocatedMemory
//				jobs.add(job);
//				allocatedMemory += new Integer(job.getParams().get("memory"));
//			}
//		}

		// Create the schedule
		Schedule schedule = new Schedule();
		schedule.setJobs(getSender(), jobs);

		// Dispatch the schedule
		dispatchJobs(schedule);
	}

}
