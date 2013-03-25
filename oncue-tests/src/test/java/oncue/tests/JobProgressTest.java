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

import java.util.Arrays;

import junit.framework.Assert;
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.JobProgress;
import oncue.tests.base.AbstractActorSystemTest;
import oncue.tests.workers.TestWorker;

import org.junit.Test;

import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;

/**
 * When a job has been scheduled, the worker may elect to send back regular
 * reports about job completion progress.
 */
public class JobProgressTest extends AbstractActorSystemTest {

	@Test
	public void monitorProgress() {
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

				// Create a queue manager
				ActorRef queueManager = createQueueManager(system, null);

				// Create a scheduler with a probe
				createScheduler(system, schedulerProbe.getRef());

				// Enqueue a job
				queueManager.tell(new EnqueueJob(TestWorker.class.getName()), getRef());

				// Start an agent with a probe
				createAgent(system, Arrays.asList(TestWorker.class.getName()), agentProbe.getRef());

				// Expect a series of progress reports
				double expectedProgress = 0;
				for (int i = 0; i < 5; i++) {
					JobProgress agentProgress = agentProbe.expectMsgClass(JobProgress.class);
					JobProgress schedulerProgress = schedulerProbe.expectMsgClass(JobProgress.class);
					Assert.assertEquals(expectedProgress, agentProgress.getProgress());
					Assert.assertEquals(expectedProgress, schedulerProgress.getProgress());
					expectedProgress += 0.25;
				}

				// Expect no more progress
				schedulerProbe.expectNoMsg();
			}
		};
	}
}
