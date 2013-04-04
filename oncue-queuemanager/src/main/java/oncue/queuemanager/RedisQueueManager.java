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

import oncue.backingstore.RedisBackingStore;
import oncue.common.messages.Job;

/**
 * A persistent, Redis-backed queue manager implementation. This component
 * depends on {@linkplain RedisBackingStore} to obtain connections to Redis,
 * from a connection pool.
 */
public class RedisQueueManager extends AbstractQueueManager {

	@Override
	protected Job createJob(String workerType, Map<String, String> jobParams) {

		// Create a new job
		Job job = RedisBackingStore.createJob(workerType, jobParams);

		getLog().debug("Enqueueing {} for worker {}", job, workerType);

		// Tell the scheduler about it
		getContext().actorFor(getSettings().SCHEDULER_PATH).tell(job, getSelf());

		return job;
	}
}
