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

import java.util.List;
import java.util.Map;

import oncue.backingstore.RedisBackingStore;
import oncue.messages.internal.Job;
import oncue.queueManager.internal.AbstractQueueManager;

import org.joda.time.DateTime;

import redis.clients.jedis.Jedis;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * A persistent, Redis-backed queue manager implementation. This component
 * depends on {@linkplain RedisBackingStore} to obtain connections to Redis,
 * from a connection pool.
 */
public class RedisQueueManager extends AbstractQueueManager {

	/**
	 * This component is responsible for monitoring a Redis-backed queue using
	 * the 'BRPOP' command, which blocks until a new item arrives on the queue.
	 */
	static class QueueMonitor extends UntypedActor {

		private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

		public QueueMonitor() {
			log.info("The Redis Queue Monitor is watching for new jobs");
			Jedis redis = RedisBackingStore.getConnection();
			while (true) {
				List<String> jobIDs = redis.brpop(0, RedisBackingStore.NEW_JOBS_QUEUE);
				Job job = RedisBackingStore.loadJob(new Long(jobIDs.get(1)), redis);
				log.debug("Found a new job: {}", job);
				getContext().parent().tell(job, getSelf());
			}
		}

		@Override
		public void onReceive(Object message) throws Exception {
		}
	}

	private ActorRef queueMonitor;

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

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof Job) {
			// Tell the scheduler about it
			getContext().actorFor(getSettings().SCHEDULER_PATH).tell(message, getSelf());
		}
		super.onReceive(message);
	}

	@Override
	public void postStop() {
		super.postStop();
		getContext().stop(queueMonitor);
	}

	@Override
	public void preStart() {
		super.preStart();
		queueMonitor = getContext().actorOf(new Props(QueueMonitor.class), "queueMonitor");
	}

}
