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
package oncue.queueManager;

import static akka.dispatch.Futures.future;
import static akka.pattern.Patterns.pipe;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import oncue.backingstore.RedisBackingStore;
import oncue.messages.internal.Job;
import oncue.queueManager.internal.AbstractQueueManager;

import org.joda.time.DateTime;

import redis.clients.jedis.Jedis;
import scala.concurrent.Future;

/**
 * A persistent, Redis-backed queue manager implementation. This component
 * depends on {@linkplain RedisBackingStore} to obtain connections to Redis,
 * from a connection pool.
 */
public class RedisQueueManager extends AbstractQueueManager {

	@Override
	protected Job createJob(String workerType, Map<String, String> jobParams) {

		// Get a connection to Redis
		Jedis redis = RedisBackingStore.getConnection();

		// Get the latest job ID
		Long jobId = redis.incr(RedisBackingStore.JOB_COUNT_KEY);

		// Create a new job
		Job job = new Job(jobId, DateTime.now(), workerType, jobParams);

		// Now, persist the job
		RedisBackingStore.persistJob(job, RedisBackingStore.NEW_JOBS_QUEUE, redis);

		RedisBackingStore.releaseConnection(redis);

		return job;
	}

	/**
	 * Monitor the new jobs queue using the Redis BRPOP command, which blocks
	 * until a new item arrives on the queue.
	 */
	private void listenForNewJobs() {
		Future<Job> jobListener = future(new Callable<Job>() {
			public Job call() {
				Jedis redis = RedisBackingStore.getConnection();
				List<String> jobIDs = redis.brpop(0, RedisBackingStore.NEW_JOBS_QUEUE);
				Job job = RedisBackingStore.loadJob(new Long(jobIDs.get(1)), redis);
				return job;
			}
		}, getContext().dispatcher());
		pipe(jobListener, getContext().dispatcher()).to(getSelf());
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof Job) {
			log.debug("Found a new job: {}", message);

			// Tell the scheduler about it
			getContext().actorFor(getSettings().SCHEDULER_PATH).tell(message, getSelf());

			// Keep listening for new jobs
			listenForNewJobs();
		}
		super.onReceive(message);
	}

	@Override
	public void preStart() {
		super.preStart();
		listenForNewJobs();
	}

}
