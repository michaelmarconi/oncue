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
package oncue.robustness;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.Arrays;

import oncue.agent.UnlimitedCapacityAgent;
import oncue.base.AbstractActorSystemTest;
import oncue.messages.internal.EnqueueJob;
import oncue.messages.internal.Job;
import oncue.messages.internal.JobFailed;
import oncue.queueManager.InMemoryQueueManager;
import oncue.scheduler.SimpleQueuePopScheduler;
import oncue.workers.IncompetentTestWorker;
import oncue.workers.TestWorker;

import org.junit.Test;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;

/**
 * An {@linkplain IWorker} may die while it is trying to complete a job. In this
 * case, it should be shut down gracefully and the problem noted by the
 * scheduler.
 */
public class WorkerDiesTest extends AbstractActorSystemTest {

	@Test
	@SuppressWarnings("serial")
	public void testWorkerDies() {
		new JavaTestKit(system) {
			{
				// Create a scheduler probe
				final JavaTestKit schedulerProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {

							@Override
							protected boolean ignore(Object message) {
								return !(message instanceof JobFailed);
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
						SimpleQueuePopScheduler scheduler = new SimpleQueuePopScheduler(null);
						scheduler.injectProbe(schedulerProbe.getRef());
						return scheduler;
					}
				}), settings.SCHEDULER_NAME);

				// Create and expose an agent
				TestActorRef<UnlimitedCapacityAgent> agentRef = TestActorRef.create(system, new Props(
						new UntypedActorFactory() {
							@Override
							public Actor create() throws Exception {
								return new UnlimitedCapacityAgent(Arrays.asList(TestWorker.class.getName()));
							}
						}), settings.AGENT_NAME);
				final UnlimitedCapacityAgent agent = agentRef.underlyingActor();

				// Enqueue a job
				queueManager.tell(new EnqueueJob(IncompetentTestWorker.class.getName()), getRef());
				Job job = expectMsgClass(Job.class);

				// Expect a job failure message at the scheduler
				JobFailed jobFailed = schedulerProbe.expectMsgClass(JobFailed.class);
				Job failedJob = jobFailed.getJob();

				assertEquals("Job IDs don't match", job.getId(), failedJob.getId());
				assertTrue("Wrong exception type", jobFailed.getError() instanceof ArithmeticException);

				// Check that there are no workers
				new AwaitCond(duration("5 second"), duration("1 second")) {

					@Override
					protected boolean cond() {
						return !agent.getContext().getChildren().iterator().hasNext();
					}
				};
			}
		};
	}

}
