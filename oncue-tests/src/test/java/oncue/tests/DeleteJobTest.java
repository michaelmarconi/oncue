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

import java.util.Arrays;
import java.util.HashSet;

import oncue.common.exceptions.DeleteJobException;
import oncue.common.messages.DeleteJob;
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.JobProgress;
import oncue.common.messages.UnmodifiableJob.State;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.TestWorker;

import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.Status.Failure;
import akka.testkit.JavaTestKit;

public class DeleteJobTest extends ActorSystemTest {

	@Test
	public void deleteJob() {
		new JavaTestKit(system) {
			{
				// Create a scheduler
				ActorRef scheduler = createScheduler(system, null);

				// Enqueue a job
				scheduler.tell(new EnqueueJob(TestWorker.class.getName()), getRef());

				// Expect a response from the scheduler
				Job job = expectMsgClass(Job.class);

				// Delete the job
				scheduler.tell(new DeleteJob(job.getId()), getRef());

				// Expect a response from the scheduler
				Job deletedJob = expectMsgClass(Job.class);

				assertEquals(job.getId(), deletedJob.getId());
				assertEquals(Job.State.DELETED, deletedJob.getState());
			}
		};
	}

	@Test
	public void deleteRunningJob() {
		new JavaTestKit(system) {
			{
				// Create a scheduler with a probe
				final JavaTestKit schedulerProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {
							@Override
							protected boolean ignore(Object message) {
								return !(message instanceof JobProgress);
							}
						};
					}
				};
				ActorRef scheduler = createScheduler(system, schedulerProbe.getRef());

				// Create an agent
				createAgent(system, new HashSet<String>(Arrays.asList(TestWorker.class.getName())), null);

				// Enqueue a job
				scheduler.tell(new EnqueueJob(TestWorker.class.getName()), getRef());

				// Expect a response from the scheduler
				Job job = expectMsgClass(Job.class);

				// Wait for job progress
				JobProgress progress = schedulerProbe.expectMsgClass(JobProgress.class);
				if (progress.getJob().getState() != State.RUNNING)
					throw new RuntimeException("The job must be running to test this scenario");

				// Delete the job
				scheduler.tell(new DeleteJob(job.getId()), getRef());

				// Expect a response from the scheduler
				Failure failure = expectMsgClass(Failure.class);
				assertEquals(DeleteJobException.class, failure.cause().getClass());
			}
		};
	}

}
