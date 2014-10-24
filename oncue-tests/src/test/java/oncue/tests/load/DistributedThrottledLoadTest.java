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
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import oncue.backingstore.RedisBackingStore;
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.JobProgress;
import oncue.tests.base.DistributedActorSystemTest;
import oncue.tests.load.workers.SimpleLoadTestWorker;

import org.junit.Ignore;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;

/**
 * Test the "job throttling" strategy, which combines the
 * {@linkplain ThrottledScheduler} and {@linkplain ThrottledAgent} to ensure
 * that a limited number of jobs can be processed by the agent at any one time.
 * 
 * This test sets up two separate actor systems and uses Netty to remote between
 * them.
 * 
 * TODO Re-instate this test
 * This may cause a mvn:test exception.  Use this command to try an isolate the issue:
 * "mvn -Dtest=DistributedThrottledLoadTest,MissingWorkerTest,RedisBackingStoreTest test"
 */
public class DistributedThrottledLoadTest extends DistributedActorSystemTest {

	private static final int JOB_COUNT = 20000;

	@Test
	@Ignore("Performance issues need to be cured before we get this running again.")
	public void distributedThrottledLoadTest() {

		// Create a queue manager probe
		final JavaTestKit queueManagerProbe = new JavaTestKit(serviceSystem);

		// Create a scheduler probe
		final JavaTestKit schedulerProbe = new JavaTestKit(serviceSystem) {
			{
				new IgnoreMsg() {

					@Override
					protected boolean ignore(Object message) {
						return !(message instanceof JobProgress || message instanceof EnqueueJob || message instanceof JobSummary);
					}
				};
			}
		};

		// Create a throttled, Redis-backed scheduler with a probe
		final ActorRef scheduler = createScheduler(schedulerProbe.getRef());

		serviceLog.info("Enqueing {} jobs...", JOB_COUNT);

		// Enqueue a stack of jobs
		for (int i = 0; i < JOB_COUNT; i++) {
			scheduler.tell(new EnqueueJob(SimpleLoadTestWorker.class.getName()),
					queueManagerProbe.getRef());
			queueManagerProbe.expectMsgClass(Job.class);
		}

		// Wait for all jobs to be enqueued
		new AwaitCond(duration("60 seconds"), duration("5 seconds")) {

			@Override
			protected boolean cond() {
				scheduler.tell(SimpleMessage.JOB_SUMMARY, queueManagerProbe.getRef());
				JobSummary summary = queueManagerProbe.expectMsgClass(JobSummary.class);
				return summary.getJobs().size() == JOB_COUNT;
			}
		};

		serviceLog.info("Jobs enqueued.");

		// Create a throttled agent
		createAgent(new HashSet<String>(Arrays.asList(SimpleLoadTestWorker.class.getName())), null);

		// Wait until all the jobs have completed
		new AwaitCond(duration("5 minutes"), duration("5 seconds")) {

			@Override
			protected boolean cond() {
				scheduler.tell(SimpleMessage.JOB_SUMMARY, queueManagerProbe.getRef());
				@SuppressWarnings("cast")
				JobSummary summary = (JobSummary) queueManagerProbe.expectMsgClass(JobSummary.class);
				int completed = 0;
				for (Job job : summary.getJobs()) {
					if (job.getState() == State.COMPLETE) {
						completed++;
					}
				}
				return completed == JOB_COUNT;
			}
		};

		serviceLog.info("All jobs were processed!");

	}

}