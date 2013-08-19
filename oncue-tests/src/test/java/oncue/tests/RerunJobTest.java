/*******************************************************************************
 * Copyright 2013 Michael Marconi
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package oncue.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.JobProgress;
import oncue.common.messages.RerunJob;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.RerunTestWorker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;

/**
 * If a job is re-run, the worker needs to perform the 'redoWork' method,
 * instead of the 'doWork' method.
 */
public class RerunJobTest extends ActorSystemTest {

	@Before @After
	public void cleanRedis() {
		log.info("Cleaning out Redis");
		Jedis jedis = new Jedis("localhost");
		jedis.del("oncue.tests.workers.RerunTestworker");
	}

	@Test
	public void rerunJob() {
		new JavaTestKit(system) {
			{
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

				// Create a scheduler with a probe
				ActorRef scheduler = createScheduler(system, schedulerProbe.getRef());

				// Start an agent
				createAgent(system, new HashSet<String>(Arrays.asList(RerunTestWorker.class.getName())), null);

				// Enqueue a job
				scheduler.tell(new EnqueueJob(RerunTestWorker.class.getName()), null);

				// Expect job to run
				JobProgress jobProgress = schedulerProbe.expectMsgClass(JobProgress.class);
				assertEquals(Job.State.RUNNING, jobProgress.getJob().getState());
				assertFalse(jobProgress.getJob().isRerun());

				// Expect job to be complete
				jobProgress = schedulerProbe.expectMsgClass(JobProgress.class);
				assertEquals(Job.State.COMPLETE, jobProgress.getJob().getState());

				// Re-run job
				scheduler.tell(new RerunJob(jobProgress.getJob().getId()), null);

				// Expect job to run again
				jobProgress = schedulerProbe.expectMsgClass(JobProgress.class);
				assertEquals(Job.State.RUNNING, jobProgress.getJob().getState());
				assertTrue(jobProgress.getJob().isRerun());

				// Expect job to be complete
				jobProgress = schedulerProbe.expectMsgClass(JobProgress.class);
				assertEquals(Job.State.COMPLETE, jobProgress.getJob().getState());

				// Now check for evidence in Redis
				Jedis jedis = new Jedis("localhost");
				String value = jedis.get("oncue.tests.workers.RerunTestworker");
				assertEquals("re-run", value);
			}
		};
	}
}
