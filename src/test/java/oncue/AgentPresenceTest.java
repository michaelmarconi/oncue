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
package oncue;

import java.util.Arrays;

import oncue.agent.UnlimitedCapacityAgent;
import oncue.base.AbstractActorSystemTest;
import oncue.messages.internal.SimpleMessages.SimpleMessage;
import oncue.scheduler.SimpleQueuePopScheduler;
import oncue.workers.TestWorker;

import org.junit.Test;

import sun.management.Agent;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Scheduler;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;

/**
 * Agents should emit a steady heartbeat while they are alive. When Agents die,
 * they are automatically deregistered at the Scheduler.
 */
public class AgentPresenceTest extends AbstractActorSystemTest {

	/**
	 * An {@linkplain Agent} should emit a steady heartbeat while it is alive.
	 */
	@SuppressWarnings("serial")
	@Test
	public void agentEmitsHeartbeat() {
		new JavaTestKit(system) {
			{
				// Create a simple scheduler with a probe
				system.actorOf(new Props(new UntypedActorFactory() {
					@Override
					public Actor create() throws Exception {
						SimpleQueuePopScheduler scheduler = new SimpleQueuePopScheduler(null);
						scheduler.injectProbe(getRef());
						return scheduler;
					}
				}), settings.SCHEDULER_NAME);

				// Ignore everything except heartbeats
				new IgnoreMsg() {
					protected boolean ignore(Object message) {
						return !message.equals(SimpleMessage.AGENT_HEARTBEAT);
					}
				};

				// Create an agent
				ActorRef agent = system.actorOf(new Props(new UntypedActorFactory() {
					@Override
					public Actor create() throws Exception {
						return new UnlimitedCapacityAgent(Arrays.asList(TestWorker.class.getName()));
					}
				}), settings.AGENT_NAME);

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
	 * When an {@linkplain Agent} dies, it should eventually be de-registered
	 * from the {@linkplain Scheduler}
	 */
	@SuppressWarnings("serial")
	@Test
	public void agentDies() {
		new JavaTestKit(system) {
			{
				// Create a simple scheduler with a probe
				system.actorOf(new Props(new UntypedActorFactory() {
					@Override
					public Actor create() throws Exception {
						SimpleQueuePopScheduler scheduler = new SimpleQueuePopScheduler(null);
						scheduler.injectProbe(getRef());
						return scheduler;
					}
				}), settings.SCHEDULER_NAME);

				// Ignore everything except heartbeats and dead agents
				new IgnoreMsg() {
					protected boolean ignore(Object message) {
						if (message.equals(SimpleMessage.AGENT_HEARTBEAT) || message.equals(SimpleMessage.AGENT_DEAD))
							return false;
						else
							return true;
					}
				};

				// Create an agent
				ActorRef agent = system.actorOf(new Props(new UntypedActorFactory() {
					@Override
					public Actor create() throws Exception {
						return new UnlimitedCapacityAgent(Arrays.asList(TestWorker.class.getName()));
					}
				}), settings.AGENT_NAME);

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
