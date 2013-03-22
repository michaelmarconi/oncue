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
import oncue.common.messages.SimpleMessages.SimpleMessage;
import oncue.service.queuemanager.InMemoryQueueManager;
import oncue.service.scheduler.SimpleQueuePopScheduler;
import oncue.tests.base.AbstractActorSystemTest;
import oncue.tests.workers.TestWorker;

import org.junit.Test;

import sun.management.Agent;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Scheduler;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;

/**
 * When the {@linkplain Scheduler} has unscheduled jobs, it broadcasts a
 * "Work available" message repeatedly, until all the unscheduled jobs have been
 * taken by an {@linkplain Agent}.
 */
public class BroadcastWorkTest extends AbstractActorSystemTest {

	@Test
	@SuppressWarnings("serial")
	public void oneJobToScheduleButNoAgents() {
		new JavaTestKit(system) {
			{
				// Create a queue manager
				ActorRef queueManager = system.actorOf(new Props(InMemoryQueueManager.class),
						settings.QUEUE_MANAGER_NAME);

				// Create a scheduler
				final JavaTestKit schedulerProbe = new JavaTestKit(system);
				system.actorOf(new Props(new UntypedActorFactory() {
					@Override
					public Actor create() throws Exception {
						SimpleQueuePopScheduler scheduler = new SimpleQueuePopScheduler(null);
						scheduler.injectProbe(schedulerProbe.getRef());
						return scheduler;
					}
				}), "scheduler");

				// Enqueue a job
				queueManager.tell(new EnqueueJob(TestWorker.class.getName()), getRef());

				// Expect the Scheduler to see the new Job
				schedulerProbe.expectMsgClass(Job.class);

				// Expect no broadcast, as there are no agents
				schedulerProbe.expectNoMsg();
			}
		};
	}

	@Test
	@SuppressWarnings("serial")
	public void oneJobToScheduleAndOneAgent() {
		new JavaTestKit(system) {
			{
				// Create a scheduler probe
				final JavaTestKit schedulerProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {
							protected boolean ignore(Object message) {
								if (message instanceof AbstractWorkRequest || message instanceof Job)
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
								if (message instanceof WorkResponse || message.equals(SimpleMessage.WORK_AVAILABLE))
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

				// Expect the broadcast about the job
				agentProbe.expectMsgEquals(SimpleMessage.WORK_AVAILABLE);
			}
		};
	}

}
