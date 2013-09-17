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
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.WorkAvailable;
import oncue.common.messages.WorkResponse;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.TestWorker;

import org.junit.Test;

import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;

/**
 * When the Scheduler has unscheduled jobs, it broadcasts a "Work available"
 * message repeatedly, until all the unscheduled jobs have been taken by an
 * Agent.
 */
public class BroadcastWorkTest extends ActorSystemTest {

	@Test
	public void oneJobToScheduleButNoAgents() {
		new JavaTestKit(system) {
			{
				// Create a scheduler with a probe
				final JavaTestKit schedulerProbe = new JavaTestKit(system);
				ActorRef scheduler = createScheduler(system, schedulerProbe.getRef());

				// Enqueue a job
				scheduler.tell(new EnqueueJob(TestWorker.class.getName()), getRef());

				// Expect the Scheduler to see the new Job
				schedulerProbe.expectMsgClass(EnqueueJob.class);

				// Expect no broadcast, as there are no agents
				schedulerProbe.expectNoMsg();
			}
		};
	}

	@Test
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
								if (message instanceof WorkResponse || message instanceof WorkAvailable)
									return false;
								else
									return true;
							}
						};
					}
				};

				// Create a scheduler with a probe
				ActorRef scheduler = createScheduler(system, schedulerProbe.getRef());

				// Create an agent
				createAgent(system, new HashSet<String>(Arrays.asList(TestWorker.class.getName())), agentProbe.getRef());

				// Wait until the agent receives an empty work response
				WorkResponse workResponse = agentProbe.expectMsgClass(WorkResponse.class);
				assertEquals(0, workResponse.getJobs().size());

				// Enqueue a job
				scheduler.tell(new EnqueueJob(TestWorker.class.getName()), getRef());

				// Expect the broadcast about the job
				agentProbe.expectMsgClass(WorkAvailable.class);
			}
		};
	}

}
