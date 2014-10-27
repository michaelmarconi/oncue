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
package oncue.tests.load;

import java.util.Arrays;
import java.util.HashSet;

import oncue.agent.ThrottledAgent;
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.Job.State;
import oncue.common.messages.JobSummary;
import oncue.common.messages.SimpleMessages.SimpleMessage;
import oncue.scheduler.ThrottledScheduler;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.load.workers.SimpleLoadTestWorker;

import org.junit.Test;

import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;

/**
 * Test the "job throttling" strategy, which combines the
 * {@linkplain ThrottledScheduler} and {@linkplain ThrottledAgent} to ensure
 * that a limited number of jobs can be processed by the agent at any one time.
 */
public class ThrottledLoadTest extends ActorSystemTest {

	private static final int JOB_COUNT = 10000;

	@Test
	public void throttledLoadTest() {
		new JavaTestKit(system) {
			{
				// Create a scheduler probe
				final JavaTestKit schedulerProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {

							@Override
							protected boolean ignore(Object message) {
								return !(message instanceof EnqueueJob);
							}
						};
					}
				};

				// Create a throttled, memory-backed scheduler with a probe
				final ActorRef scheduler = createScheduler(system, schedulerProbe.getRef());

				log.info("Enqueing {} jobs...", JOB_COUNT);

				// Enqueue a stack of jobs
				for (int i = 0; i < JOB_COUNT; i++) {
					scheduler.tell(new EnqueueJob(SimpleLoadTestWorker.class.getName()), null);
				}

				// Wait for all jobs to be enqueued
				new AwaitCond() {

					@Override
					protected boolean cond() {
						scheduler.tell(SimpleMessage.JOB_SUMMARY, getRef());
						JobSummary summary = expectMsgClass(JobSummary.class);
						return summary.getJobs().size() == JOB_COUNT;
					}
				};

				log.info("Jobs enqueued.");

				// Create a throttled agent
				createAgent(system,
						new HashSet<String>(Arrays.asList(SimpleLoadTestWorker.class.getName())),
						null);

				// Wait until all the jobs have completed
				new AwaitCond(duration("60 seconds"), duration("5 seconds")) {

					@Override
					protected boolean cond() {
						scheduler.tell(SimpleMessage.JOB_SUMMARY, getRef());
						@SuppressWarnings("cast")
						JobSummary summary = (JobSummary) expectMsgClass(JobSummary.class);
						int completed = 0;
						for (Job job : summary.getJobs()) {
							if (job.getState() == State.COMPLETE) {
								completed++;
							}
						}

						return completed == JOB_COUNT;
					}
				};

				log.info("All jobs were processed!");
			}
		};
	}
}
