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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

import java.util.Arrays;

import oncue.agent.UnlimitedCapacityAgent;
import oncue.base.AbstractActorSystemTest;
import oncue.messages.internal.EnqueueJob;
import oncue.messages.internal.Job;
import oncue.messages.internal.JobProgress;
import oncue.messages.internal.WorkResponse;
import oncue.queueManager.InMemoryQueueManager;
import oncue.scheduler.SimpleQueuePopScheduler;
import oncue.workers.TestWorker;

import org.junit.Test;

import sun.management.Agent;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;

/**
 * When an {@linkplain Agent} receives a {@linkplain WorkResponse}, it will
 * attempt to spawn an instance of an {@linkplain IWorker} for each
 * {@linkplain Job} in the list.
 */
public class WorkerTest extends AbstractActorSystemTest {

	@Test
	@SuppressWarnings("serial")
	public void spawnWorkerAndStartJob() {
		new JavaTestKit(system) {
			{
				// Create an agent probe
				final JavaTestKit agentProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {
							protected boolean ignore(Object message) {
								if (message instanceof WorkResponse || message instanceof JobProgress) {
									return false;
								} else {
									return true;
								}
							}
						};
					}
				};

				// Create a queue manager
				ActorRef queueManager = system.actorOf(new Props(InMemoryQueueManager.class),
						settings.QUEUE_MANAGER_NAME);

				// Create a simple scheduler
				system.actorOf(new Props(new UntypedActorFactory() {

					@Override
					public Actor create() throws Exception {
						return new SimpleQueuePopScheduler(null);
					}
				}), settings.SCHEDULER_NAME);

				// Create and expose an agent
				TestActorRef<UnlimitedCapacityAgent> agentRef = TestActorRef.create(system, new Props(
						new UntypedActorFactory() {
							@Override
							public Actor create() throws Exception {
								UnlimitedCapacityAgent agent = new UnlimitedCapacityAgent(Arrays
										.asList(TestWorker.class.getName()));
								agent.injectProbe(agentProbe.getRef());
								return agent;
							}
						}), settings.AGENT_NAME);
				UnlimitedCapacityAgent agent = agentRef.underlyingActor();

				// Wait until the agent receives an empty work response
				WorkResponse workResponse = agentProbe.expectMsgClass(WorkResponse.class);
				assertEquals(0, workResponse.getJobs().size());

				// Check that there are no workers
				assertFalse("Expected no child workers", agent.getContext().getChildren().iterator().hasNext());

				// Enqueue a job
				queueManager.tell(new EnqueueJob(TestWorker.class.getName()), getRef());

				// Expect a work response
				agentProbe.expectMsgClass(WorkResponse.class);

				// Expect a Job Progress message, showing no progress
				JobProgress jobProgress = agentProbe.expectMsgClass(JobProgress.class);
				assertEquals("Expected no progress on the job", 0.0, jobProgress.getProgress());

				final ActorRef worker = agent.getContext().getChildren().iterator().next();

				// Wait for the job to complete
				new AwaitCond(duration("5 seconds")) {
					@Override
					protected boolean cond() {
						return agentProbe.expectMsgClass(JobProgress.class).getProgress() == 1.0;
					}
				};

				// Ensure the worker is dead
				new AwaitCond(duration("5 seconds")) {
					@Override
					protected boolean cond() {
						return worker.isTerminated() == true;
					}
				};
			}
		};
	}
}
