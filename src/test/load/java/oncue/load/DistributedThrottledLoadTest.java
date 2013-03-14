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
package oncue.load;

import junit.framework.Assert;
import oncue.agent.ThrottledAgent;
import oncue.backingstore.RedisBackingStore;
import oncue.messages.internal.EnqueueJob;
import oncue.messages.internal.Job;
import oncue.messages.internal.JobProgress;
import oncue.queueManager.InMemoryQueueManager;
import oncue.scheduler.ThrottledScheduler;
import oncue.settings.Settings;
import oncue.settings.SettingsProvider;
import oncue.workers.SimpleLoadTestWorker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.testkit.JavaTestKit;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Test the "job throttling" strategy, which combines the
 * {@linkplain ThrottledScheduler} and {@linkplain ThrottledAgent} to ensure
 * that a limited number of jobs can be processed by the agent at any one time.
 * 
 * This test will wait until one or more remote agents contacts the scheduler
 * for work.
 */
public class DistributedThrottledLoadTest {

	protected static Config config;
	protected final ActorSystem system;
	protected final Settings settings;
	protected LoggingAdapter log;
	private static final int JOB_COUNT = 10000;

	public DistributedThrottledLoadTest() {
		config = ConfigFactory.load("distributed-throttled-load-test.conf");
		system = ActorSystem.create("oncue-service", config.getConfig("service").withFallback(config));
		settings = SettingsProvider.SettingsProvider.get(system);
		log = Logging.getLogger(system, this);
	}

	@Before
	public void cleanRedis() {
		Jedis redis = RedisBackingStore.getConnection();
		redis.flushDB();
		RedisBackingStore.releaseConnection(redis);
	}

	@Test
	@SuppressWarnings("serial")
	public void simpleLoadTest() {
		new JavaTestKit(system) {
			{
				// Create an in-memory queue manager
				ActorRef queueManager = system.actorOf(new Props(InMemoryQueueManager.class),
						settings.QUEUE_MANAGER_NAME);

				// Create a scheduler probe
				final JavaTestKit schedulerProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {

							@Override
							protected boolean ignore(Object message) {
								return !(message instanceof JobProgress);
							}
						};
					}
				};

				// Create a throttled, Redis-backed scheduler
				system.actorOf(new Props(new UntypedActorFactory() {
					@Override
					public Actor create() throws Exception {
						ThrottledScheduler scheduler = new ThrottledScheduler(RedisBackingStore.class);
						scheduler.injectProbe(schedulerProbe.getRef());
						return scheduler;
					}
				}), settings.SCHEDULER_NAME);

				// Enqueue a stack of jobs
				for (int i = 0; i < JOB_COUNT; i++) {
					queueManager.tell(new EnqueueJob(SimpleLoadTestWorker.class.getName()), getRef());
					expectMsgClass(Job.class);
				}

				log.info("All jobs enqueued. Go ahead and start some agents...");

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

				RedisBackingStore.releaseConnection(redis);
			}
		};
	}

	@After
	public void tearDown() {
		system.shutdown();
		Jedis redis = RedisBackingStore.getConnection();
		redis.flushDB();
		RedisBackingStore.releaseConnection(redis);
	}
}
