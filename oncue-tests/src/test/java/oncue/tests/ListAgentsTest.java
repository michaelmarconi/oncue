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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import oncue.common.messages.AgentSummary;
import oncue.common.messages.SimpleMessages.SimpleMessage;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.TestWorker;

import org.junit.Test;

import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;

/**
 * A scheduler can be queried about the list of registered agents.
 */
public class ListAgentsTest extends ActorSystemTest {

	/**
	 * The scheduler should respond with an empty list, since no agents have
	 * been registered.
	 * 
	 * @throws Exception
	 */
	@Test
	public void listAgentsButNoneRegistered() throws Exception {
		new JavaTestKit(system) {
			{
				// Ignore everything except agent summaries
				new IgnoreMsg() {
					protected boolean ignore(Object message) {
						return !(message instanceof AgentSummary);
					}
				};

				// Create a scheduler
				ActorRef scheduler = createScheduler(system, null);

				// Ask the scheduler about the agents
				scheduler.tell(SimpleMessage.LIST_AGENTS, getRef());

				// Expect no agents
				AgentSummary agents = expectMsgClass(AgentSummary.class);
				assertEquals(0, agents.getAgents().size());
			}
		};
	}

	/**
	 * The scheduler should respond with a single registered agent
	 * 
	 * @throws Exception
	 */
	@Test
	public void listAgentsWithOneRegistered() throws Exception {
		new JavaTestKit(system) {
			{
				// Ignore everything except agent summaries
				new IgnoreMsg() {
					protected boolean ignore(Object message) {
						return !(message instanceof AgentSummary);
					}
				};

				// Create a scheduler with a probe
				final JavaTestKit schedulerProbe = new JavaTestKit(system);
				ActorRef scheduler = createScheduler(system, schedulerProbe.getRef());

				// Create an agent and wait for the heart beat
				createAgent(system, Arrays.asList(TestWorker.class.getName()), null);
				schedulerProbe.expectMsgEquals(SimpleMessage.AGENT_HEARTBEAT);

				// Ask the scheduler about the agents
				scheduler.tell(SimpleMessage.LIST_AGENTS, getRef());

				// Expect a single agent
				AgentSummary agents = expectMsgClass(AgentSummary.class);
				assertEquals(1, agents.getAgents().size());
				assertEquals("akka://oncue-test/user/agent", agents.getAgents().get(0).getUrl());
			}
		};
	}
}
