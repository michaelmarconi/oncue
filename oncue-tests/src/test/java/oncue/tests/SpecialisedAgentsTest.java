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

import static junit.framework.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;

import oncue.common.messages.AbstractWorkRequest;
import oncue.common.messages.EnqueueJob;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.TestWorker;
import oncue.tests.workers.TestWorker2;

import org.junit.Test;

import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;

/**
 * Agents can be specialised to operate with only a subset of available worker
 * types. In this case, they should only respond to 'work available' messages
 * that they are capable of spawning a worker for.
 */
public class SpecialisedAgentsTest extends ActorSystemTest {

	/**
	 * An agent should only respond to a work available message that is has the
	 * worker types to work on.
	 */
	@Test
	public void agentsOnlyRespondToWorkTheyCanHandle() {
		new JavaTestKit(system) {
			{
				// Create a queue manager
				ActorRef queueManager = createQueueManager(system);

				// Create a scheduler with a probe
				final JavaTestKit schedulerProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {
							protected boolean ignore(Object message) {
								if (message instanceof AbstractWorkRequest)
									return false;
								else
									return true;
							}
						};
					}
				};
				createScheduler(system, schedulerProbe.getRef());

				// Create an agent that can run "TestWorker" workers
				ActorRef agent1 = createAgent(system, new HashSet<String>(Arrays.asList(TestWorker.class.getName())));

				// Create an agent that can run "TestWorker2" workers
				ActorRef agent2 = createAgent(system, new HashSet<String>(Arrays.asList(TestWorker2.class.getName())));

				// Expect an initial work requests
				schedulerProbe.expectMsgClass(AbstractWorkRequest.class);
				schedulerProbe.expectMsgClass(AbstractWorkRequest.class);
				schedulerProbe.expectNoMsg(duration("1 second"));

				// ---

				// Enqueue a job for TestWorker
				queueManager.tell(new EnqueueJob(TestWorker.class.getName()), getRef());

				// Expect work request from agent 1
				AbstractWorkRequest workRequest = schedulerProbe.expectMsgClass(AbstractWorkRequest.class);
				assertEquals(agent1, workRequest.getAgent());

				// Expect follow-up work request from agent 1
				workRequest = schedulerProbe.expectMsgClass(AbstractWorkRequest.class);
				assertEquals(agent1, workRequest.getAgent());

				// Expect no message from agent 2
				schedulerProbe.expectNoMsg();

				// ---

				// Enqueue a job for TestWorker2
				queueManager.tell(new EnqueueJob(TestWorker2.class.getName()), getRef());

				// Expect work request from agent 2
				workRequest = schedulerProbe.expectMsgClass(AbstractWorkRequest.class);
				assertEquals(agent2, workRequest.getAgent());

				// Expect follow-up work request from agent 2
				workRequest = schedulerProbe.expectMsgClass(AbstractWorkRequest.class);
				assertEquals(agent2, workRequest.getAgent());

				// Expect no message from agent 1
				schedulerProbe.expectNoMsg();
			}
		};
	}
}
