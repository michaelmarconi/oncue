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
import java.util.HashSet;

import oncue.common.messages.SimpleMessages.SimpleMessage;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.TestWorker;

import org.junit.Test;

import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;

/**
 * Agents should emit a steady heartbeat while they are alive. When Agents die,
 * they are automatically deregistered at the Scheduler.
 */
public class AgentPresenceTest extends ActorSystemTest {

	/**
	 * An agent should emit a steady heartbeat while it is alive.
	 */
	@Test
	public void agentEmitsHeartbeat() {
		new JavaTestKit(system) {
			{
				// Ignore everything except heartbeats
				new IgnoreMsg() {
					protected boolean ignore(Object message) {
						return !message.equals(SimpleMessage.AGENT_HEARTBEAT);
					}
				};

				// Create a scheduler with a probe
				createScheduler(system, getRef());

				// Create an agent
				ActorRef agent = createAgent(system, new HashSet<String>(Arrays.asList(TestWorker.class.getName())),
						null);

				// Expect the initial heartbeat
				expectMsgEquals(SimpleMessage.AGENT_HEARTBEAT);

				// Wait for a second heartbeat
				expectMsgEquals(settings.AGENT_HEARTBEAT_FREQUENCY.plus(duration("2 seconds")),
						SimpleMessage.AGENT_HEARTBEAT);

				// Stop the Agent and associated heartbeat
				system.stop(agent);
				expectNoMsg(settings.AGENT_HEARTBEAT_FREQUENCY);
			}
		};
	}

	/**
	 * When an agent dies, it should eventually be de-registered from the
	 * scheduler.
	 */
	@Test
	public void agentDies() {
		new JavaTestKit(system) {
			{
				// Ignore everything except heartbeats and dead agents
				new IgnoreMsg() {
					protected boolean ignore(Object message) {
						if (message.equals(SimpleMessage.AGENT_HEARTBEAT) || message.equals(SimpleMessage.AGENT_DEAD))
							return false;
						else
							return true;
					}
				};

				// Create a scheduler with a probe
				createScheduler(system, getRef());

				// Create an agent
				ActorRef agent = createAgent(system, new HashSet<String>(Arrays.asList(TestWorker.class.getName())),
						null);

				// Expect the initial heartbeat
				expectMsgEquals(SimpleMessage.AGENT_HEARTBEAT);

				// Stop the Agent
				system.stop(agent);

				// Wait for a "dead agent" message
				expectMsgEquals(duration("30 seconds"), SimpleMessage.AGENT_DEAD);
			}
		};
	}
}
