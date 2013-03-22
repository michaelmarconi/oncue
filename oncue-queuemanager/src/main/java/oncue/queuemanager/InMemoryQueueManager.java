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
package oncue.queuemanager;

import java.util.Map;

import oncue.common.messages.Job;

import org.joda.time.DateTime;

/**
 * A simple, in-memory implementation of a queue manager. Since this queue
 * manager has no backing store, it immediately tells the scheduler about the
 * new job.
 */
public class InMemoryQueueManager extends AbstractQueueManager {

	private long currentJobID = 0;

	@Override
	protected Job createJob(String workerType, Map<String, String> jobParams) {

		// Create a new job
		Job job = new Job(getNextJobID(), DateTime.now(), workerType, jobParams);
		
		getLog().debug("Enqueueing {} for worker {}", job,  workerType);

		// Tell the scheduler about it
		getContext().actorFor(getSettings().SCHEDULER_PATH).tell(job, getSelf());

		return job;
	}

	private long getNextJobID() {
		currentJobID++;
		return currentJobID;
	}
}
