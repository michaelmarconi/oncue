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
package oncue.tests;

import static akka.pattern.Patterns.gracefulStop;
import static junit.framework.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.JobProgress;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.TestWorker;

import org.junit.Test;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;

/**
 * When a job has been scheduled, the worker may elect to send back regular
 * reports about job completion progress.
 */
public class JobProgressTest extends ActorSystemTest {

	@Test
	public void monitorProgress() throws Exception {
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

				// Create an agent probe
				final JavaTestKit agentProbe = new JavaTestKit(system) {
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

				// Enqueue a job
				scheduler.tell(new EnqueueJob(TestWorker.class.getName()), getRef());

				// Start an agent with a probe
				ActorRef agent = createAgent(system, new HashSet<String>(Arrays.asList(TestWorker.class.getName())),
						agentProbe.getRef());

				// Expect a series of progress reports
				double expectedProgress = 0;
				for (int i = 0; i < 4; i++) {

					// Agent progress
					JobProgress agentProgress = agentProbe.expectMsgClass(JobProgress.class);
					assertEquals("Was expecting progress at the Agent of " + expectedProgress, expectedProgress,
							agentProgress.getJob().getProgress());
					assertEquals("Was expecting the job to be running.", Job.State.RUNNING, agentProgress.getJob()
							.getState());

					// Scheduler progress
					JobProgress schedulerProgress = schedulerProbe.expectMsgClass(JobProgress.class);
					assertEquals("Was expecting progress at the Scheduler of " + expectedProgress, expectedProgress,
							schedulerProgress.getJob().getProgress());
					assertEquals("Was expecting the job to be running.", Job.State.RUNNING, schedulerProgress.getJob()
							.getState());

					expectedProgress += 0.25;
				}

				// Expect the final completion message
				JobProgress schedulerProgress = schedulerProbe.expectMsgClass(JobProgress.class);
				assertEquals(1.0, schedulerProgress.getJob().getProgress());
				assertEquals(Job.State.COMPLETE, schedulerProgress.getJob().getState());

				// Expect no more messages
				schedulerProbe.expectNoMsg();

				// The agent should shut down first to prevent lookup exceptions
				Future<Boolean> stopped = gracefulStop(agent, duration("5 seconds"), system);
				Await.result(stopped, Duration.create(5, TimeUnit.SECONDS));

			}
		};
	}
}
