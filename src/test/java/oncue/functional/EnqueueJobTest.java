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

import static org.junit.Assert.assertEquals;
import oncue.base.AbstractActorSystemTest;
import oncue.messages.internal.EnqueueJob;
import oncue.messages.internal.Job;
import oncue.queueManager.InMemoryQueueManager;
import oncue.scheduler.SimpleQueuePopScheduler;
import oncue.workers.TestWorker;

import org.junit.Test;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;

public class EnqueueJobTest extends AbstractActorSystemTest {

	@Test
	@SuppressWarnings("serial")
	public void testEnqueuingJob() {
		new JavaTestKit(system) {
			{
				// Create a queue manager
				ActorRef queueManager = system.actorOf(new Props(InMemoryQueueManager.class),
						settings.QUEUE_MANAGER_NAME);

				// Create a scheduler probe
				final JavaTestKit schedulerProbe = new JavaTestKit(system);

				// Create a simple scheduler with a probe
				system.actorOf(new Props(new UntypedActorFactory() {
					@Override
					public Actor create() throws Exception {
						SimpleQueuePopScheduler scheduler = new SimpleQueuePopScheduler(null);
						scheduler.injectProbe(schedulerProbe.getRef());
						return scheduler;
					}
				}), "scheduler");

				for (int i = 0; i < 2; i++) {

					// Enqueue a job
					queueManager.tell(new EnqueueJob(TestWorker.class.getName()), getRef());
					Job job = expectMsgClass(Job.class);

					assertEquals("The job has the wrong ID", i + 1, job.getId());
					assertEquals("The job has the worker type", TestWorker.class.getName(), job.getWorkerType());

					// Expect the job at the scheduler
					Job schedulerJob = schedulerProbe.expectMsgClass(Job.class);

					// Compare the jobs to ensure they are the same
					assertEquals("Didn't find the expected job", job, schedulerJob);
				}
			}
		};
	}

}
