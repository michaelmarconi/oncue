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
import java.util.List;

import oncue.agent.MissingWorkerException;
import oncue.common.messages.AbstractWorkRequest;
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.JobFailed;
import oncue.tests.base.AbstractActorSystemTest;
import oncue.tests.workers.TestWorker;
import oncue.worker.AbstractWorker;

import org.junit.Test;

import akka.actor.ActorRef;
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
 * <h3>Scheduler dispatched job to wrong agent</h3>
 * 
 * <p>
 * When an agent makes a request for work, it tells the scheduler what worker
 * types it is capable of spawning. If the scheduler ignores this and requests
 * that the agent complete a job it has no worker for, the job should fail.
 * </p>
 */
public class MissingWorkerTest extends AbstractActorSystemTest {

	@Test
	public void startAgentWithMissingWorkerType() {
		new JavaTestKit(system) {
			{
				// Start an agent
				createAgent(system, Arrays.asList("oncue.workers.MissingWorker"), null);

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

	@Test
	public void scheduleJobToUnqualifiedAgent() {
		new JavaTestKit(system) {
			{
				// Create a scheduler probe
				final JavaTestKit schedulerProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {
							protected boolean ignore(Object message) {
								if (message instanceof AbstractWorkRequest || message instanceof JobFailed)
									return false;
								else
									return true;
							}
						};
					}
				};

				// Create a queue manager
				ActorRef queueManager = createQueueManager(system, null);

				// Create a scheduler with a probe
				createScheduler(system, schedulerProbe.getRef());

				// Create an agent
				createAgent(system, Arrays.asList(TestWorker.class.getName()), null);

				// Expect agent to report available worker types
				AbstractWorkRequest workRequest = schedulerProbe.expectMsgClass(AbstractWorkRequest.class);
				assertEquals(1, workRequest.getWorkerTypes().size());
				assertEquals(TestWorker.class.getName(), ((List<String>) workRequest.getWorkerTypes()).get(0));

				// Enqueue a job
				queueManager.tell(new EnqueueJob("oncue.workers.MissingWorker"), getRef());
				expectMsgClass(Job.class);

				// Expect a work request
				schedulerProbe.expectMsgClass(AbstractWorkRequest.class);

				// Expect the job to fail
				JobFailed jobFailed = schedulerProbe.expectMsgClass(JobFailed.class);
				assertEquals(MissingWorkerException.class, jobFailed.getError().getClass());
				assertEquals(ClassNotFoundException.class, jobFailed.getError().getCause().getClass());
			}
		};
	}
}
