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

import java.util.Arrays;

import oncue.agent.UnlimitedCapacityAgent;
import oncue.base.AbstractActorSystemTest;
import oncue.workers.internal.AbstractWorker;

import org.junit.Test;

import sun.management.Agent;
import akka.actor.Actor;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;

/**
 * When an {@linkplain Agent} is instantiated, it must be passed a list of
 * worker classes that must extend the {@linkplain AbstractWorker} class. This
 * test ensures that the agent system will shut down if this is not the case.
 */
public class MissingWorkerTypeTest extends AbstractActorSystemTest {

	@Test
	@SuppressWarnings("serial")
	public void startAgentWithMissingWorkerType() {
		new JavaTestKit(system) {
			{
				// Create an agent
				system.actorOf(new Props(new UntypedActorFactory() {
					@Override
					public Actor create() throws Exception {
						return new UnlimitedCapacityAgent(Arrays.asList("oncue.workers.MissingWorker"));
					}
				}), settings.AGENT_NAME);

				// Wait for the system to shut down
				new AwaitCond(duration("5 seconds"), duration("1 second")) {
					@Override
					protected boolean cond() {
						return system.isTerminated();
					}
				};
			}
		};
	}
}
