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
package oncue.tests.schedulers;

import static junit.framework.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;

import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.WorkResponse;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.TestWorker;
import oncue.tests.workers.TestWorker2;

import org.junit.Test;

import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;

/**
 * Tests that are specific to throttled schedulers
 */
public class SimpleQueuePopSchedulerTest extends ActorSystemTest {

	/**
	 * A simple queue-pop scheduler should only schedule jobs to agents that
	 * have the appropriate worker types.
	 */
	@SuppressWarnings("unused")
	@Test
	public void scheduleJobsToCapableAgents() {
		new JavaTestKit(system) {
			{
				// Create a queue manager
				ActorRef queueManager = createQueueManager(system);

				// Create a scheduler
				createScheduler(system);

				// Enqueue jobs
				queueManager.tell(new EnqueueJob(TestWorker.class.getName()), getRef());
				expectMsgClass(Job.class);
				queueManager.tell(new EnqueueJob(TestWorker2.class.getName()), getRef());
				expectMsgClass(Job.class);

				// ---

				// Create an agent that can run "TestWorker" workers
				final JavaTestKit agent1Probe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {
							protected boolean ignore(Object message) {
								if (message instanceof WorkResponse)
									return false;
								else
									return true;
							}
						};
					}
				};
				createAgent(system, new HashSet<String>(Arrays.asList(TestWorker.class.getName())),
						agent1Probe.getRef());

				// Expect a work response with only job 1 at agent 1
				WorkResponse workResponse1 = agent1Probe.expectMsgClass(WorkResponse.class);
				assertEquals(1, workResponse1.getJobs().size());
				assertEquals(1, workResponse1.getJobs().get(0).getId());

				// Wait for 'No jobs' work response after work is complete
				workResponse1 = agent1Probe.expectMsgClass(WorkResponse.class);
				assertEquals(0, workResponse1.getJobs().size());

				// ---

				// Create an agent that can run "TestWorker2" workers
				final JavaTestKit agent2Probe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {
							protected boolean ignore(Object message) {
								if (message instanceof WorkResponse)
									return false;
								else
									return true;
							}
						};
					}
				};
				createAgent(system, new HashSet<String>(Arrays.asList(TestWorker2.class.getName())),
						agent2Probe.getRef());

				// Expect a work response with only job 2 at agent 2
				WorkResponse workResponse2 = agent2Probe.expectMsgClass(WorkResponse.class);
				assertEquals(1, workResponse2.getJobs().size());
				assertEquals(2, workResponse2.getJobs().get(0).getId());

				// Wait for 'No jobs' work response after work is complete
				workResponse2 = agent2Probe.expectMsgClass(WorkResponse.class);
				assertEquals(0, workResponse2.getJobs().size());
			}
		};
	}
}
