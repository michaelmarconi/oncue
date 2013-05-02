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

import oncue.tests.base.ActorSystemTest;

import org.junit.Test;

import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.testkit.JavaTestKit;

/**
 * <p>
 * There are two scenarios here:
 * </p>
 * 
 * <h3>Agent is missing a worker class at boot time</h3>
 * 
 * <p>
 * When an agent is instantiated, it must be passed a list of worker classes
 * that must extend the {@linkplain AbstractWorker} class. This test ensures
 * that the agent system will shut down if this is not the case.
 * </p>
 * 
 */
public class MissingWorkerTest extends ActorSystemTest {

	@Test
	public void startAgentWithMissingWorkerType() {
		new JavaTestKit(system) {
			{
				LoggingAdapter log = Logging.getLogger(system, this);

				// Start an agent
				createAgent(system, new HashSet<String>(Arrays.asList("oncue.workers.MissingWorker")), null);

				// Wait for the system to shut down
				new AwaitCond(duration("5 seconds"), duration("1 second")) {
					@Override
					protected boolean cond() {
						return system.isTerminated();
					}
				};

				log.info("Agent system shut down as expected");
			}
		};
	}
}
