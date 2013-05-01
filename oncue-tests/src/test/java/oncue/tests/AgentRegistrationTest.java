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
import oncue.common.messages.SimpleMessages.SimpleMessage;
import oncue.common.messages.WorkResponse;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.TestWorker;

import org.junit.Test;

import akka.testkit.JavaTestKit;

/**
 * Agents should register themselves with the central Scheduler when they start
 * up. After receiving confirmation that they are registered, they should
 * request work.
 */
public class AgentRegistrationTest extends ActorSystemTest {

	/**
	 * An agent should emit a steady heartbeat while it is alive.
	 */
	@Test
	public void agentRegistersAndRequestsWorkButReceivesNoWork() {
		new JavaTestKit(system) {
			{
				// Create a scheduler with a probe
				final JavaTestKit schedulerProbe = new JavaTestKit(system);
				createScheduler(system, schedulerProbe.getRef());

				// Create an agent with a probe
				final JavaTestKit agentProbe = new JavaTestKit(system);
				createAgent(system, new HashSet<String>(Arrays.asList(TestWorker.class.getName())), agentProbe.getRef());

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
