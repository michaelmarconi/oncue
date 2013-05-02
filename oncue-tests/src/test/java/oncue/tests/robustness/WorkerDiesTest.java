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
package oncue.tests.robustness;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import oncue.agent.ThrottledAgent;
import oncue.agent.UnlimitedCapacityAgent;
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.JobFailed;
import oncue.common.messages.JobProgress;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.IncompetentTestWorker;
import oncue.tests.workers.TestWorker;

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
public class WorkerDiesTest extends ActorSystemTest {

	@SuppressWarnings("unused")
	@Test
	public void testWorkerDies() {
		new JavaTestKit(system) {
			{
				// Create a scheduler probe
				final JavaTestKit schedulerProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {

							@Override
							protected boolean ignore(Object message) {
								if(message instanceof JobFailed || message instanceof JobProgress)
									return false;

								return true;
							}
						};
					}
				};

				// Create a queue manager
				ActorRef queueManager = createQueueManager(system, null);

				// Create a scheduler with a probe
				createScheduler(system, schedulerProbe.getRef());

				// Create and expose an agent
				@SuppressWarnings("serial")
				TestActorRef<ThrottledAgent> agentRef = TestActorRef.create(system, new Props(
						new UntypedActorFactory() {
							@Override
							public Actor create() throws Exception {
								return new ThrottledAgent(new HashSet<String>(Arrays
										.asList(TestWorker.class.getName(), IncompetentTestWorker.class.getName())));
							}
						}), settings.AGENT_NAME);
				final ThrottledAgent agent = agentRef.underlyingActor();

				// Enqueue a job
				queueManager.tell(new EnqueueJob(IncompetentTestWorker.class.getName()), getRef());
				Job job = expectMsgClass(Job.class);

				// Expect some initial progress
				schedulerProbe.expectMsgClass(JobProgress.class); // 0
				schedulerProbe.expectMsgClass(JobProgress.class); // 0.25

				// Expect a job failure message at the scheduler
				JobFailed jobFailed = schedulerProbe.expectMsgClass(JobFailed.class);
				Job failedJob = jobFailed.getJob();

				assertEquals("Job IDs don't match", job.getId(), failedJob.getId());
				assertTrue("Wrong exception type", jobFailed.getJob().getErrorMessage().contains(ArithmeticException.class.getName()));

				// Check that there are no workers
				new AwaitCond(duration("5 second"), duration("1 second")) {

					@Override
					protected boolean cond() {
						return !agent.getContext().getChildren().iterator().hasNext();
					}
				};

				// Enqueue a job
				queueManager.tell(new EnqueueJob(TestWorker.class.getName()), getRef());
				job = expectMsgClass(Job.class);

				// Expect a job progress message at the scheduler
				schedulerProbe.expectMsgClass(JobProgress.class);

			}
		};
	}

}
