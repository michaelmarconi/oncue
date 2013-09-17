/*******************************************************************************
 * Copyright 2013 Michael Marconi
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package oncue.tests;

import java.util.Arrays;
import java.util.HashSet;

import oncue.common.messages.SimpleMessages.SimpleMessage;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.TestWorker;

import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;

import com.typesafe.config.Config;

/**
 * When a remote agent shuts down gracefully (i.e. a final remote client
 * shutdown event is broadcast), the scheduler should note this and de-register
 * the agent, before it becomes a dead agent.
 */
public class AgentShutdownTest extends ActorSystemTest {

	private ActorSystem agentSystem;

	/**
	 * We need to create a separate actor system for the agent to inhabit for
	 * this test
	 */
	@Before
	public void createAgentActorSystem() {
		Config agentConfig = config.getConfig("agent").withFallback(config);
		agentSystem = ActorSystem.create("oncue-agent", agentConfig);
	}

	/**
	 * An agent should emit a steady heartbeat while it is alive.
	 */
	@Test
	public void agentShutsDownGracefully() {
		new JavaTestKit(system) {

			{
				final JavaTestKit schedulerProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {
							@Override
							protected boolean ignore(Object message) {
								return !(message.equals(SimpleMessage.AGENT_SHUTDOWN));
							}
						};
					}
				};

				final JavaTestKit agentProbe = new JavaTestKit(system);

				createScheduler(system, schedulerProbe.getRef());
				createAgent(agentSystem, new HashSet<String>(Arrays.asList(TestWorker.class.getName())),
						agentProbe.getRef());

				// Wait until the agent is registered
				agentProbe.expectMsgEquals(SimpleMessage.AGENT_REGISTERED);

				// Tell the agent to stop
				agentSystem.shutdown();

				// Expect the agent shutdown message
				schedulerProbe.expectMsgEquals(duration("5 seconds"), SimpleMessage.AGENT_SHUTDOWN);
			}
		};
	}
}
