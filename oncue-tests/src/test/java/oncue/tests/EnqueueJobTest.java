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

import static org.junit.Assert.assertEquals;
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.TestWorker;

import org.junit.Test;

import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;

public class EnqueueJobTest extends ActorSystemTest {

	@Test
	public void testEnqueuingJob() {
		new JavaTestKit(system) {
			{
				// Create a scheduler
				ActorRef scheduler = createScheduler(system, null);

				for (int i = 0; i < 2; i++) {

					// Enqueue a job
					scheduler.tell(new EnqueueJob(TestWorker.class.getName()), getRef());

					// Expect a response from the scheduler
					Job job = expectMsgClass(Job.class);
					assertEquals("The job has the wrong ID", i + 1, job.getId());
					assertEquals("The job has the worker type", TestWorker.class.getName(), job.getWorkerType());
				}
			}
		};
	}

}
