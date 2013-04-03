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
package oncue.tests.redis;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oncue.backingstore.RedisBackingStore;
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.JobFailed;
import oncue.common.messages.JobProgress;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.IncompetentTestWorker;
import oncue.tests.workers.TestWorker;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;

public class RedisBackingStoreTest extends ActorSystemTest {

	@Test
	public void addScheduledJob() {
		new JavaTestKit(system) {
			{
				// Create a scheduler probe
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

				// Create a queue manager
				ActorRef queueManager = createQueueManager(system, null);

				// Create a Redis-backed scheduler (see config) with a probe
				createScheduler(system, schedulerProbe.getRef());

				// Construct job params
				Map<String, String> params = new HashMap<>();
				params.put("month", "Jan");
				params.put("size", "x-large");

				// Enqueue a job
				queueManager.tell(new EnqueueJob(TestWorker.class.getName(), params), getRef());
				Job job = expectMsgClass(Job.class);

				// Start an Agent
				createAgent(system, Arrays.asList(TestWorker.class.getName()), null);

				// Wait for some progress
				schedulerProbe.expectMsgClass(JobProgress.class);

				// Check to see that scheduled job has been recorded in Redis
				Jedis redis = RedisBackingStore.getConnection();
				List<String> jobIDs = redis.brpop(0, RedisBackingStore.SCHEDULED_JOBS);
				long jobId = new Long(jobIDs.get(1));
				Job loadedJob = RedisBackingStore.loadJob(jobId, redis);

				assertEquals(job.getId(), loadedJob.getId());
				assertEquals(job.getEnqueuedAt().toString(), loadedJob.getEnqueuedAt().toString());
				assertEquals(job.getWorkerType(), loadedJob.getWorkerType());
				assertEquals("Wrong number of parameters", 2, loadedJob.getParams().size());
				assertEquals(job.getParams().get("month"), loadedJob.getParams().get("month"));
				assertEquals(job.getParams().get("size"), loadedJob.getParams().get("size"));
			}
		};
	}

	@Test
	public void addUnscheduledJob() {
		new JavaTestKit(system) {
			{
				// Create a queue manager
				ActorRef queueManager = createQueueManager(system, null);

				// Create a Redis-backed scheduler (see config)
				createScheduler(system, null);

				// Construct job params
				Map<String, String> params = new HashMap<>();
				params.put("month", "Jan");
				params.put("size", "x-large");

				// Enqueue a job
				queueManager.tell(new EnqueueJob(TestWorker.class.getName(), params), getRef());
				Job job = expectMsgClass(Job.class);

				// Check to see that unscheduled job has been recorded in Redis
				Jedis redis = RedisBackingStore.getConnection();
				List<String> jobIDs = redis.brpop(0, RedisBackingStore.UNSCHEDULED_JOBS);
				long jobId = new Long(jobIDs.get(1));
				Job loadedJob = RedisBackingStore.loadJob(jobId, redis);

				assertEquals(job.getId(), loadedJob.getId());
				assertEquals(job.getEnqueuedAt().toString(), loadedJob.getEnqueuedAt().toString());
				assertEquals(job.getWorkerType(), loadedJob.getWorkerType());
				assertEquals("Wrong number of parameters", 2, loadedJob.getParams().size());
				assertEquals(job.getParams().get("month"), loadedJob.getParams().get("month"));
				assertEquals(job.getParams().get("size"), loadedJob.getParams().get("size"));
				assertEquals("There should be no more jobs on the unscheduled queue", 0,
						redis.llen(RedisBackingStore.UNSCHEDULED_JOBS).longValue());
			}
		};
	}

	@Before
	@After
	public void cleanRedis() {
		Jedis redis = RedisBackingStore.getConnection();
		redis.flushDB();
		RedisBackingStore.releaseConnection(redis);
	}

	@Test
	public void persistJobFailure() {
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
				ActorRef queueManager = createQueueManager(system, null);

				// Create a Redis-backed scheduler (see config) with a probe
				createScheduler(system, schedulerProbe.getRef());

				// Enqueue a job for an incompetent worker
				queueManager.tell(new EnqueueJob(IncompetentTestWorker.class.getName()), getRef());
				Job job = expectMsgClass(Job.class);

				// Start an Agent
				createAgent(system, Arrays.asList(TestWorker.class.getName()), null);

				// Expect a job failure message at the scheduler
				JobFailed jobFailed = schedulerProbe.expectMsgClass(JobFailed.class);
				Job failedJob = jobFailed.getJob();
				assertEquals("Job IDs don't match", job.getId(), failedJob.getId());
				assertTrue("Wrong exception type", jobFailed.getError() instanceof ArithmeticException);

				expectNoMsg();

				// Check to see that job failure has been recorded in Redis
				Jedis redis = RedisBackingStore.getConnection();
				final String jobKey = String.format(RedisBackingStore.JOB_KEY, job.getId());
				String state = redis.hget(jobKey, RedisBackingStore.JOB_STATE);
				String stacktrace = redis.hget(jobKey, RedisBackingStore.JOB_FAILURE_STACKTRACE);
				RedisBackingStore.releaseConnection(redis);

				assertNotNull("No job state found", state);
				assertEquals("The recorded state does not match the expected state", Job.State.FAILED.toString(), state);
				assertTrue("Stacktrace is wayward", stacktrace.contains("ArithmeticException"));
			}
		};
	}

	@Test
	public void persistJobProgress() {
		new JavaTestKit(system) {
			{
				// Create a scheduler probe
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

				// Create a queue manager
				ActorRef queueManager = createQueueManager(system, null);

				// Create a Redis-backed scheduler (see config) with a probe
				createScheduler(system, schedulerProbe.getRef());

				// Enqueue a job
				queueManager.tell(new EnqueueJob(TestWorker.class.getName()), getRef());
				Job job = expectMsgClass(Job.class);

				// Start an Agent
				createAgent(system, Arrays.asList(TestWorker.class.getName()), null);

				// Expect a series of progress reports
				double expectedProgress = 0;
				for (int i = 0; i < 5; i++) {
					JobProgress jobProgress = schedulerProbe.expectMsgClass(JobProgress.class);
					assertEquals(expectedProgress, jobProgress.getProgress());
					expectedProgress += 0.25;
				}

				schedulerProbe.expectNoMsg();

				// Check to see progress has been recorded in Redis
				Jedis redis = RedisBackingStore.getConnection();
				final String jobKey = String.format(RedisBackingStore.JOB_KEY, job.getId());
				String progress = redis.hget(jobKey, RedisBackingStore.JOB_PROGRESS);

				assertNotNull("No progress found", progress);
				assertEquals("The recorded progress does not match the expected progress", 1.0, new Double(progress));

				RedisBackingStore.releaseConnection(redis);
			}
		};
	}

	@Test
	public void popUnscheduledJob() {
		Jedis redis = RedisBackingStore.getConnection();
		RedisBackingStore backingStore = new RedisBackingStore(null, null);

		// Push a job into Redis
		Job job = new Job(1, DateTime.now(), TestWorker.class.getName());
		RedisBackingStore.persistJob(job, RedisBackingStore.UNSCHEDULED_JOBS, redis);

		long jobID = backingStore.popUnscheduledJob();
		Job poppedJob = RedisBackingStore.loadJob(jobID, redis);

		assertEquals(job.getId(), poppedJob.getId());
		assertEquals(job.getEnqueuedAt().toString(), poppedJob.getEnqueuedAt().toString());
		assertEquals(job.getWorkerType(), poppedJob.getWorkerType());

		RedisBackingStore.releaseConnection(redis);
	}

	@Test
	public void removeScheduledJob() {
		Jedis redis = RedisBackingStore.getConnection();
		RedisBackingStore backingStore = new RedisBackingStore(null, null);

		// Push a job into Redis
		Job job = new Job(1, DateTime.now(), TestWorker.class.getName());
		RedisBackingStore.persistJob(job, RedisBackingStore.SCHEDULED_JOBS, redis);

		// Remove the scheduled job
		backingStore.removeScheduledJob(job);

		// Check scheduled list in Redis
		assertEquals("Expected no jobs in the scheduled jobs list", 0,
				redis.lrange(RedisBackingStore.SCHEDULED_JOBS, 0, -1).size());

		RedisBackingStore.releaseConnection(redis);
	}

	@Test
	public void removeUnscheduledJob() {
		Jedis redis = RedisBackingStore.getConnection();
		RedisBackingStore backingStore = new RedisBackingStore(null, null);

		// Push a job into Redis
		Job job = new Job(1, DateTime.now(), TestWorker.class.getName());
		RedisBackingStore.persistJob(job, RedisBackingStore.UNSCHEDULED_JOBS, redis);

		// Remove the scheduled job
		backingStore.removeUnscheduledJob(job);

		// Check scheduled list in Redis
		assertEquals("Expected no jobs in the unscheduled jobs list", 0,
				redis.lrange(RedisBackingStore.UNSCHEDULED_JOBS, 0, -1).size());

		RedisBackingStore.releaseConnection(redis);
	}

	@Test
	public void restoreJobs() {
		Jedis redis = RedisBackingStore.getConnection();
		RedisBackingStore backingStore = new RedisBackingStore(null, null);

		// Push an unscheduled job into Redis
		Job unscheduledJob = new Job(1, DateTime.now(), TestWorker.class.getName());
		RedisBackingStore.persistJob(unscheduledJob, RedisBackingStore.UNSCHEDULED_JOBS, redis);

		// Push a scheduled job into Redis
		Job scheduledJob = new Job(2, DateTime.now(), TestWorker.class.getName());
		RedisBackingStore.persistJob(scheduledJob, RedisBackingStore.SCHEDULED_JOBS, redis);

		// Restore the jobs
		List<Job> restoredJobs = backingStore.restoreJobs();

		// Check the set of restored jobs
		assertTrue(restoredJobs.size() == 2);
		for (Job job : restoredJobs) {
			assertTrue(job.getId() == unscheduledJob.getId() || job.getId() == scheduledJob.getId());
		}

		// Make sure no scheduled jobs remain
		assertEquals(0, redis.lrange(RedisBackingStore.SCHEDULED_JOBS, 0, -1).size());

		RedisBackingStore.releaseConnection(redis);
	}

	@Test
	public void saveAndLoadJob() {

		// Construct job params
		Map<String, String> params = new HashMap<>();
		params.put("month", "Jan");
		params.put("size", "x-large");

		Job job = new Job(0, DateTime.now(), TestWorker.class.getName(), params);
		job.setProgress(0.8);
		Jedis redis = RedisBackingStore.getConnection();
		RedisBackingStore.persistJob(job, "test_queue", redis);
		Job loadedJob = RedisBackingStore.loadJob(0, redis);
		RedisBackingStore.releaseConnection(redis);

		assertEquals(job.getId(), loadedJob.getId());
		assertEquals(job.getEnqueuedAt().toString(), loadedJob.getEnqueuedAt().toString());
		assertEquals(job.getWorkerType(), loadedJob.getWorkerType());
		assertEquals(job.getProgress(), loadedJob.getProgress());
		assertEquals("Wrong number of parameters", 2, loadedJob.getParams().size());
		assertEquals(job.getParams().get("month"), loadedJob.getParams().get("month"));
		assertEquals(job.getParams().get("size"), loadedJob.getParams().get("size"));
	}
}
