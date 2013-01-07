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
package oncue.redis;

import static junit.framework.Assert.assertEquals;

import oncue.backingstore.RedisBackingStore;
import oncue.base.AbstractActorSystemTest;
import oncue.messages.internal.EnqueueJob;
import oncue.messages.internal.Job;
import oncue.queueManager.RedisQueueManager;
import oncue.scheduler.SimpleQueuePopScheduler;
import oncue.worker.TestWorker;

import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;

/**
 * Test to ensure that jobs can be enqueued to Redis. This queue manager
 * implementation also has a Redis queue monitor, which blocks until new jobs
 * are found.
 */
public class RedisQueueManagerTest extends AbstractActorSystemTest {
	
	@Before
	public void cleanRedis()
	{
		Jedis redis = RedisBackingStore.getConnection();
		redis.flushDB();
		RedisBackingStore.releaseConnection(redis);
	}

	@Test
	@SuppressWarnings("serial")
	public void testEnqueueNewJob() {
		new JavaTestKit(system) {
			{
				// Create a Redis-backed queue manager
				ActorRef queueManager = system.actorOf(new Props(RedisQueueManager.class),
						settings.QUEUE_MANAGER_NAME);
				
				// Create a scheduler with a probe
				final JavaTestKit schedulerProbe = new JavaTestKit(system);
				system.actorOf(new Props(new UntypedActorFactory() {
					@Override
					public Actor create() throws Exception {
						SimpleQueuePopScheduler scheduler = new SimpleQueuePopScheduler(null);
						scheduler.injectProbe(schedulerProbe.getRef());
						return scheduler;
					}
				}), settings.SCHEDULER_NAME);

				// Enqueue a job
				queueManager.tell(new EnqueueJob(TestWorker.class.getName()), getRef());
				Job job = expectMsgClass(Job.class);

				// Expect the scheduler to see the new job
				Job schedulerJob = schedulerProbe.expectMsgClass(Job.class);

				assertEquals(job.getId(), schedulerJob.getId());
				assertEquals(job.getEnqueuedAt().toString(), schedulerJob.getEnqueuedAt().toString());
				assertEquals(job.getWorkerType(), schedulerJob.getWorkerType());
			}
		};
	}
}
