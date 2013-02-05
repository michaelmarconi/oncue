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

import java.util.Map;
import java.util.concurrent.Callable;

import oncue.backingstore.RedisBackingStore;
import oncue.messages.internal.Job;
import oncue.queueManager.internal.AbstractQueueManager;
import redis.clients.jedis.Jedis;
import scala.concurrent.Future;

/**
 * A persistent, Redis-backed queue manager implementation. This component
 * depends on {@linkplain RedisBackingStore} to obtain connections to Redis,
 * from a connection pool.
 */
public class RedisQueueManager extends AbstractQueueManager {

	private Jedis redis;

	@Override
	protected Job createJob(String workerType, Map<String, String> jobParams) {
		return RedisBackingStore.createJob(workerType, jobParams);
	}

	/**
	 * Monitor the new jobs queue using the Redis the 'BRPOPLPUSH' command,
	 * which blocks until a new item arrives on the queue. The new job is
	 * atomically popped/pushed on the "unscheduled jobs" queue, to prevent a
	 * job being lost if the service goes down.
	 */
	private void listenForNewJobs() {
		Future<Job> jobListener = future(new Callable<Job>() {
			public Job call() {
				redis = RedisBackingStore.getConnection();
				String jobId = redis.brpoplpush(RedisBackingStore.NEW_JOBS, RedisBackingStore.UNSCHEDULED_JOBS, 0);
				return RedisBackingStore.loadJob(new Long(jobId), redis);
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
	public void postStop() {
		super.postStop();

		/*
		 * Break the connection to redis, in order to stop the blocking job
		 * listener!
		 */
		redis.disconnect();
		RedisBackingStore.releaseConnection(redis);
		log.debug("Released blocking Redis connection");

	}

	@Override
	public void preStart() {
		super.preStart();
		listenForNewJobs();
	}

}
