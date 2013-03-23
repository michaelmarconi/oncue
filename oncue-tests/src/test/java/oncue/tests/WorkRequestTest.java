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

import oncue.agent.UnlimitedCapacityAgent;
import oncue.common.messages.AbstractWorkRequest;
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.WorkResponse;
import oncue.queuemanager.InMemoryQueueManager;
import oncue.scheduler.SimpleQueuePopScheduler;
import oncue.tests.base.AbstractActorSystemTest;
import oncue.tests.workers.TestWorker;

import org.junit.Test;

import sun.management.Agent;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;

/**
 * When an {@linkplain Agent} has received a broadcast stating that work is
 * available, it will respond by asking for work (by sending a
 * {@linkplain WorkRequest} message.
 */
public class WorkRequestTest extends AbstractActorSystemTest {

	@Test
	@SuppressWarnings("serial")
	public void requestWorkAndReceiveAJob() {
		new JavaTestKit(system) {
			{
				// Create a scheduler probe
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

				// Create an agent probe
				final JavaTestKit agentProbe = new JavaTestKit(system) {
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

				// Create a queue manager
				ActorRef queueManager = system.actorOf(new Props(InMemoryQueueManager.class),
						settings.QUEUE_MANAGER_NAME);

				// Create a simple scheduler
				system.actorOf(new Props(new UntypedActorFactory() {
					@Override
					public Actor create() throws Exception {
						SimpleQueuePopScheduler scheduler = new SimpleQueuePopScheduler(null);
						scheduler.injectProbe(schedulerProbe.getRef());
						return scheduler;
					}
				}), "scheduler");

				// Create an agent
				system.actorOf(new Props(new UntypedActorFactory() {
					@Override
					public Actor create() throws Exception {
						UnlimitedCapacityAgent agent = new UnlimitedCapacityAgent(Arrays.asList(TestWorker.class
								.getName()));
						agent.injectProbe(agentProbe.getRef());
						return agent;
					}
				}), "agent");

				// Wait until the agent receives an empty work response
				WorkResponse workResponse = agentProbe.expectMsgClass(WorkResponse.class);
				assertEquals(0, workResponse.getJobs().size());

				// Enqueue a job
				queueManager.tell(new EnqueueJob(TestWorker.class.getName()), getRef());
				Job job = expectMsgClass(Job.class);

				// Expect a request for work from the agent
				schedulerProbe.expectMsgClass(AbstractWorkRequest.class);

				// Expect a work response from the scheduler
				workResponse = agentProbe.expectMsgClass(WorkResponse.class);

				assertEquals("Expected a single job", 1, workResponse.getJobs().size());
				assertEquals("Wrong job ID", job.getId(), workResponse.getJobs().get(0).getId());
			}
		};
	}
}
