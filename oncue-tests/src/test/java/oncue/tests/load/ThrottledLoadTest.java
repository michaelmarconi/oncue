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
package oncue.tests.load;

import java.util.Arrays;
import java.util.HashSet;

import junit.framework.Assert;
import oncue.agent.ThrottledAgent;
import oncue.backingstore.RedisBackingStore;
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.JobProgress;
import oncue.scheduler.ThrottledScheduler;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.load.workers.SimpleLoadTestWorker;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;

/**
 * Test the "job throttling" strategy, which combines the
 * {@linkplain ThrottledScheduler} and {@linkplain ThrottledAgent} to ensure
 * that a limited number of jobs can be processed by the agent at any one time.
 */
public class ThrottledLoadTest extends ActorSystemTest {

	private static final int JOB_COUNT = 100000;

	@Before
	@After
	public void cleanRedis() {
		Jedis redis = RedisBackingStore.getConnection();
		redis.flushDB();
		RedisBackingStore.releaseConnection(redis);
	}

	@Test
	@Ignore("Performance issues need to be cured before we get this running again.")
	public void throttledLoadTest() {
		new JavaTestKit(system) {
			{
				// Create an in-memory queue manager
				ActorRef queueManager = createQueueManager(system, null);

				// Create a scheduler probe
				final JavaTestKit schedulerProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {

							@Override
							protected boolean ignore(Object message) {
								return !(message instanceof JobProgress || message instanceof Job);
							}
						};
					}
				};

				// Create a throttled, Redis-backed scheduler with a probe
				createScheduler(system, schedulerProbe.getRef());

				log.info("Enqueing {} jobs...", JOB_COUNT);

				// Enqueue a stack of jobs
				for (int i = 0; i < JOB_COUNT; i++) {
					queueManager.tell(new EnqueueJob(SimpleLoadTestWorker.class.getName()), null);
				}

				// Wait for all jobs to be enqueued
				for (int i = 0; i < JOB_COUNT; i++) {
					schedulerProbe.expectMsgClass(Job.class);
				}

				log.info("Jobs enqueued.");

				// Create a throttled agent
				createAgent(system, new HashSet<String>(Arrays.asList(SimpleLoadTestWorker.class.getName())), null);

				// Wait until all the jobs have completed
				final Jedis redis = RedisBackingStore.getConnection();
				new AwaitCond(duration("5 minutes"), duration("10 seconds")) {

					@Override
					protected boolean cond() {
						Job finalJob;
						try {
							finalJob = RedisBackingStore.loadJob(JOB_COUNT, redis);
							return finalJob.getProgress() == 1.0;
						} catch (RuntimeException e) {
							// Job may not exist in Redis yet
							return false;
						}
					}
				};

				// Now, check all the jobs completed in Redis
				for (int i = 0; i < JOB_COUNT; i++) {
					Job job = RedisBackingStore.loadJob(i + 1, redis);
					Assert.assertEquals(1.0, job.getProgress());
				}

				log.info("All jobs were processed!");

				RedisBackingStore.releaseConnection(redis);
			}
		};
	}
}
