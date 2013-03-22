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
import oncue.common.messages.WorkResponse;
import oncue.common.messages.SimpleMessages.SimpleMessage;
import oncue.service.scheduler.SimpleQueuePopScheduler;
import oncue.tests.base.AbstractActorSystemTest;
import oncue.tests.workers.TestWorker;

import org.junit.Test;

import sun.management.Agent;
import akka.actor.Actor;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;

/**
 * Agents should register themselves with the central Scheduler when they start
 * up. After receiving confirmation that they are registered, they should
 * request work.
 */
public class AgentRegistrationTest extends AbstractActorSystemTest {

	/**
	 * An {@linkplain Agent} should emit a steady heartbeat while it is alive.
	 */
	@SuppressWarnings("serial")
	@Test
	public void agentRegistersAndRequestsWorkButReceivesNoWork() {
		new JavaTestKit(system) {
			{
				// Create a simple scheduler with a probe
				final JavaTestKit schedulerProbe = new JavaTestKit(system);
				system.actorOf(new Props(new UntypedActorFactory() {
					@Override
					public Actor create() throws Exception {
						SimpleQueuePopScheduler scheduler = new SimpleQueuePopScheduler(null);
						scheduler.injectProbe(schedulerProbe.getRef());
						return scheduler;
					}
				}), "scheduler");

				// Create an agent with a probe
				final JavaTestKit agentProbe = new JavaTestKit(system);
				system.actorOf(new Props(new UntypedActorFactory() {
					@Override
					public Actor create() throws Exception {
						UnlimitedCapacityAgent agent = new UnlimitedCapacityAgent(Arrays.asList(TestWorker.class
								.getName()));
						agent.injectProbe(agentProbe.getRef());
						return agent;
					}
				}), "agent");

				// Expect the initial heartbeat from the agent
				schedulerProbe.expectMsgEquals(SimpleMessage.AGENT_HEARTBEAT);

				// Expect the registration message from the scheduler
				agentProbe.expectMsgEquals(SimpleMessage.AGENT_REGISTERED);

				// Expect the agent to request work
				schedulerProbe.expectMsgClass(AbstractWorkRequest.class);

				// Expect no work from the scheduler
				WorkResponse workResponse = agentProbe.expectMsgClass(WorkResponse.class);
				assertEquals("Expected no jobs", 0, workResponse.getJobs().size());
			}
		};
	}
}
