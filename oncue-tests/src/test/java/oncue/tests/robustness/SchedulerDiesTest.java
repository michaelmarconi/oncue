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
package oncue.tests.robustness;

import static akka.pattern.Patterns.gracefulStop;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import oncue.backingstore.RedisBackingStore;
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.JobProgress;
import oncue.common.messages.SimpleMessages.SimpleMessage;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.TestWorker;

import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.testkit.JavaTestKit;

/**
 * It is possible to resurrect a scheduler that was running with a persistent
 * backing store. This test ensures that we can bring a scheduler back from the
 * dead so that no jobs are lost.
 */
@SuppressWarnings("unused")
public class SchedulerDiesTest extends ActorSystemTest {

	@Test
	public void testAgentDiesAndAnotherReplacesIt() throws Exception {
		new JavaTestKit(system) {
			{
				// Create a scheduler probe
				final JavaTestKit schedulerProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {

							@Override
							protected boolean ignore(Object message) {
								if (message.equals(SimpleMessage.AGENT_DEAD) || message instanceof JobProgress)
									return false;
								else
									return true;
							}
						};
					}
				};

				// Create an agent probe
				final JavaTestKit agentProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {

							@Override
							protected boolean ignore(Object message) {
								if (message instanceof JobProgress)
									return false;
								else
									return true;
							}
						};
					}
				};

				// Create a Redis-backed scheduler with a probe
				final ActorRef scheduler = createScheduler(system, schedulerProbe.getRef());

				// Create an agent with a probe
				ActorRef agent = createAgent(system, new HashSet<String>(Arrays.asList(TestWorker.class.getName())),
						agentProbe.getRef());

				// Enqueue a job
				scheduler.tell(new EnqueueJob(TestWorker.class.getName()), getRef());
				Job job = expectMsgClass(Job.class);

				// Wait for some initial progress
				schedulerProbe.expectMsgClass(JobProgress.class);

				// Tell the scheduler to commit seppuku and wait for it to die
				log.info("Scheduler committing seppuku...");
				scheduler.tell(PoisonPill.getInstance(), getRef());
				new AwaitCond(duration("5 seconds"), duration("1 second")) {

					@Override
					protected boolean cond() {
						return scheduler.isTerminated();
					}
				};

				// Wait until the job is finished at the agent
				new AwaitCond(duration("5 seconds"), duration("1 second")) {

					@Override
					protected boolean cond() {
						JobProgress jobProgress = agentProbe.expectMsgClass(JobProgress.class);
						log.debug("Waiting for completion of {}", jobProgress.getJob());
						return jobProgress.getJob().getState() == Job.State.COMPLETE;
					}
				};

				log.debug("First run of {} complete.", job);

				// Resurrect the scheduler
				log.info("Resurrecting scheduler!");
				createScheduler(system, schedulerProbe.getRef());

				// Wait until the job is finished at the scheduler
				new AwaitCond(duration("10 seconds"), duration("1 second")) {

					@Override
					protected boolean cond() {
						JobProgress jobProgress = schedulerProbe.expectMsgClass(duration("10 seconds"),
								JobProgress.class);
						log.debug("Waiting for completion of re-scheduled {}", jobProgress.getJob());
						return jobProgress.getJob().getState() == Job.State.COMPLETE;
					}
				};
				
				// The agent should shut down first to prevent lookup exceptions
				Future<Boolean> stopped = gracefulStop(agent, duration("5 seconds"), system);
				Await.result(stopped, Duration.create(5, TimeUnit.SECONDS));
			}
		};
	}
}
