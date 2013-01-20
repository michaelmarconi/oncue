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
package oncue.functional;

import oncue.agent.UnlimitedCapacityAgent;
import oncue.messages.internal.SimpleMessages.SimpleMessage;
import oncue.scheduler.SimpleQueuePopScheduler;
import oncue.settings.Settings;
import oncue.settings.SettingsProvider;

import org.junit.Test;

import sun.management.Agent;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.testkit.JavaTestKit;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * When a remote agent shuts down gracefully (i.e. a final remote client
 * shutdown event is broadcast), the scheduler should note this and deregister
 * the agent, before it becomes a dead agent.
 */
public class AgentShutdownTest {

	static Config config;
	final ActorSystem system;
	final ActorSystem agentSystem;
	final Settings settings;
	LoggingAdapter log;

	public AgentShutdownTest() {
		config = ConfigFactory.load("agent-shutdown-test");
		system = ActorSystem.create("oncue-service", config.getConfig("service").withFallback(config));
		agentSystem = ActorSystem.create("oncue-agent", config.getConfig("client").withFallback(config));
		settings = SettingsProvider.SettingsProvider.get(system);
		log = Logging.getLogger(system, this);
	}

	/**
	 * An {@linkplain Agent} should emit a steady heartbeat while it is alive.
	 */
	@SuppressWarnings("serial")
	@Test
	public void agentShutsDownGracefully() {
		new JavaTestKit(system) {
			{
				// Create a simple scheduler with a probe
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
				ActorRef agent = agentSystem.actorOf(new Props(new UntypedActorFactory() {
					@Override
					public Actor create() throws Exception {
						UnlimitedCapacityAgent agent = new UnlimitedCapacityAgent();
						agent.injectProbe(agentProbe.getRef());
						return agent;
					}
				}), "agent");

				// Wait until the agent is registered
				agentProbe.expectMsgEquals(SimpleMessage.AGENT_REGISTERED);
				
				expectNoMsg(duration("5 seconds"));

				// Tell the agent to stop
				agentSystem.shutdown();
				
				expectNoMsg(duration("30 seconds"));

				// Expect the agent shutdown message
//				schedulerProbe.expectMsgEquals(duration("5 seconds"), SimpleMessage.AGENT_SHUTDOWN);
			}
		};
	}
}
