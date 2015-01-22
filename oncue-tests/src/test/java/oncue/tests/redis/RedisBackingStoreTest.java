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
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static oncue.backingstore.RedisBackingStore.JOB_KEY;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import oncue.backingstore.RedisBackingStore;
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.Job.State;
import oncue.common.messages.JobFailed;
import oncue.common.messages.JobProgress;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.IncompetentTestWorker;
import oncue.tests.workers.TestWorker;

import org.joda.time.DateTime;
import org.joda.time.Duration;
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

				// Create a Redis-backed scheduler (see config) with a probe
				ActorRef scheduler = createScheduler(system, schedulerProbe.getRef());

				// Construct job params
				Map<String, String> params = new HashMap<>();
				params.put("month", "Jan");
				params.put("size", "x-large");

				// Enqueue a job
				scheduler.tell(new EnqueueJob(TestWorker.class.getName(), params), getRef());
				Job job = expectMsgClass(Job.class);

				// Start an Agent
				createAgent(system, new HashSet<String>(Arrays.asList(TestWorker.class.getName())), null);

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
				// Create a Redis-backed scheduler (see config)
				ActorRef scheduler = createScheduler(system, null);

				// Construct job params
				Map<String, String> params = new HashMap<>();
				params.put("month", "Jan");
				params.put("size", "x-large");

				// Enqueue a job
				scheduler.tell(new EnqueueJob(TestWorker.class.getName(), params), getRef());
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

				// Create a Redis-backed scheduler (see config) with a probe
				ActorRef scheduler = createScheduler(system, schedulerProbe.getRef());

				// Enqueue a job for an incompetent worker
				scheduler.tell(new EnqueueJob(IncompetentTestWorker.class.getName()), getRef());
				Job job = expectMsgClass(Job.class);

				// Start an Agent
				createAgent(system, new HashSet<String>(Arrays.asList(IncompetentTestWorker.class.getName())), null);

				// Expect a job failure message at the scheduler
				JobFailed jobFailed = schedulerProbe.expectMsgClass(JobFailed.class);
				Job failedJob = jobFailed.getJob();
				assertEquals("Job IDs don't match", job.getId(), failedJob.getId());
				assertTrue("Wrong exception type",
						jobFailed.getJob().getErrorMessage().contains(ArithmeticException.class.getName()));

				expectNoMsg();

				// Check to see that job failure has been recorded in Redis
				Jedis redis = RedisBackingStore.getConnection();
				final String jobKey = String.format(RedisBackingStore.JOB_KEY, job.getId());
				String state = redis.hget(jobKey, RedisBackingStore.JOB_STATE);
				String errorMessage = redis.hget(jobKey, RedisBackingStore.JOB_ERROR_MESSAGE);
				RedisBackingStore.releaseConnection(redis);

				assertNotNull("No job state found", state);
				assertEquals("The recorded state does not match the expected state", Job.State.FAILED.toString(), state);
				assertTrue(errorMessage.contains(ArithmeticException.class.getName()));
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

				// Create a Redis-backed scheduler (see config) with a probe
				ActorRef scheduler = createScheduler(system, schedulerProbe.getRef());

				// Enqueue a job
				scheduler.tell(new EnqueueJob(TestWorker.class.getName()), getRef());
				Job job = expectMsgClass(Job.class);

				// Start an Agent
				createAgent(system, new HashSet<String>(Arrays.asList(TestWorker.class.getName())), null);

				// Expect a series of progress reports
				double expectedProgress = 0;
				for (int i = 0; i < 5; i++) {
					JobProgress jobProgress = schedulerProbe.expectMsgClass(JobProgress.class);
					assertEquals(expectedProgress, jobProgress.getJob().getProgress());
					expectedProgress += 0.25;
				}

				schedulerProbe.expectNoMsg();

				// Check to see progress has been recorded in Redis
				Jedis redis = RedisBackingStore.getConnection();
				final String jobKey = String.format(RedisBackingStore.JOB_KEY, job.getId());
				String progress = redis.hget(jobKey, RedisBackingStore.JOB_PROGRESS);

				assertNotNull("No progress found", progress);
				assertEquals("The recorded progress does not match the expected progress", 1.0, new Double(progress));

				// Check to see that only one job was persisted
				assertEquals(1, redis.lrange(RedisBackingStore.COMPLETED_JOBS, 0, -1).size());

				RedisBackingStore.releaseConnection(redis);
			}
		};
	}

	@Test
	public void getCompletedJobs() {
		new JavaTestKit(system) {
			{
				Jedis redis = RedisBackingStore.getConnection();
				RedisBackingStore backingStore = new RedisBackingStore(system, settings);

				// Push a job into Redis
				Job job = new Job(1, TestWorker.class.getName());
				RedisBackingStore.persistJob(job, RedisBackingStore.SCHEDULED_JOBS, redis);

				// Record progress on the job
				job.setProgress(1.0);
				job.setState(Job.State.COMPLETE);
				backingStore.persistJobProgress(job);

				// Get the list of completed jobs
				List<Job> completedJobs = backingStore.getCompletedJobs();

				assertEquals(1, completedJobs.size());
				assertEquals(job.getId(), completedJobs.get(0).getId());

				RedisBackingStore.releaseConnection(redis);
			}
		};
	}

	@Test
	public void getFailedJobs() {
		new JavaTestKit(system) {
			{
				Jedis redis = RedisBackingStore.getConnection();
				RedisBackingStore backingStore = new RedisBackingStore(system, settings);

				// Push a job into Redis
				Job job = new Job(1, TestWorker.class.getName());
				RedisBackingStore.persistJob(job, RedisBackingStore.SCHEDULED_JOBS, redis);

				// Record job failure on the job
				job.setErrorMessage(new Exception("Test exception").toString());
				backingStore.persistJobFailure(job);

				// Get the list of failed jobs
				List<Job> failedJobs = backingStore.getFailedJobs();

				assertEquals(1, failedJobs.size());
				assertEquals(job.getId(), failedJobs.get(0).getId());

				RedisBackingStore.releaseConnection(redis);
			}
		};
	}

	@Test
	public void restoreJobs() {
		new JavaTestKit(system) {
			{
				Jedis redis = RedisBackingStore.getConnection();
				RedisBackingStore backingStore = new RedisBackingStore(system, settings);

				// Push an unscheduled job into Redis
				Job unscheduledJob = new Job(1, TestWorker.class.getName());
				RedisBackingStore.persistJob(unscheduledJob, RedisBackingStore.UNSCHEDULED_JOBS, redis);

				// Push a scheduled job into Redis
				Job scheduledJob = new Job(2, TestWorker.class.getName());
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
		};
	}

	@Test
	public void saveAndLoadJob() {

		// Construct job params
		Map<String, String> params = new HashMap<>();
		params.put("month", "Jan");
		params.put("size", "x-large");

		DateTime testTime = DateTime.now();

		Job job = new Job(0, TestWorker.class.getName());
		job.setParams(params);
		job.setProgress(0.8);
		job.setRerun(true);
		job.setStartedAt(testTime);
		job.setCompletedAt(testTime);
		Jedis redis = RedisBackingStore.getConnection();
		RedisBackingStore.persistJob(job, "test_queue", redis);
		Job loadedJob = RedisBackingStore.loadJob(0, redis);
		RedisBackingStore.releaseConnection(redis);

		assertNotNull("Expected 'enqueued at' timestamp", job.getEnqueuedAt());
		assertNotNull("Expected 'started at' timestamp", job.getStartedAt());
		assertNotNull("Expected 'completed at' timestamp", job.getCompletedAt());

		assertEquals(job.getId(), loadedJob.getId());
		assertEquals(job.getEnqueuedAt().toString(), loadedJob.getEnqueuedAt().toString());
		assertEquals(testTime.toString(), loadedJob.getStartedAt().toString());
		assertEquals(testTime.toString(), loadedJob.getCompletedAt().toString());
		assertEquals(job.getWorkerType(), loadedJob.getWorkerType());
		assertEquals(job.getProgress(), loadedJob.getProgress());
		assertEquals(job.isRerun(), loadedJob.isRerun());
		assertEquals("Wrong number of parameters", 2, loadedJob.getParams().size());
		assertEquals(job.getParams().get("month"), loadedJob.getParams().get("month"));
		assertEquals(job.getParams().get("size"), loadedJob.getParams().get("size"));
	}

	@Test
	public void cleanUpJobsReturnsCorrectCleanedUpJobCount() {
		new JavaTestKit(system) {
			private Jedis redis;
			private RedisBackingStore backingStore;

			{
				redis = RedisBackingStore.getConnection();
				backingStore = new RedisBackingStore(system, settings);

				// Push expired jobs into redis
				persistTestJob(1, DateTime.now().minusHours(2), false);
				persistTestJob(2, DateTime.now().minusHours(2), true);
				persistTestJob(3, DateTime.now().minusHours(2), true);

				assertTrue(redis.exists(getJobId(1)));
				assertTrue(redis.exists(getJobId(2)));
				assertTrue(redis.exists(getJobId(3)));

				assertEquals(1, backingStore.cleanupJobs(false, Duration.standardHours(1)));
				assertEquals(2, backingStore.cleanupJobs(true, Duration.standardHours(1)));

				assertFalse(redis.exists(getJobId(1)));
				assertFalse(redis.exists(getJobId(2)));
				assertFalse(redis.exists(getJobId(3)));

				RedisBackingStore.releaseConnection(redis);
			}

			private void persistTestJob(int jobNumber, DateTime completionTime, boolean failed) {
				Job job = new Job(jobNumber, TestWorker.class.getName());
				job.setCompletedAt(completionTime);
				RedisBackingStore.persistJob(job, RedisBackingStore.SCHEDULED_JOBS, redis);

				if (failed) {
					job.setState(State.FAILED);
					job.setErrorMessage(new Exception("Test exception").toString());
					backingStore.persistJobFailure(job);
				} else {
					// Record progress on the job
					job.setProgress(1.0);
					job.setState(Job.State.COMPLETE);
					backingStore.persistJobProgress(job);
				}
			}
		};
	}

	@Test
	public void removeJobById() {
		new JavaTestKit(system) {
			private Jedis redis;
			private RedisBackingStore backingStore;

			{
				redis = RedisBackingStore.getConnection();
				backingStore = new RedisBackingStore(system, settings);

				Job job = new Job(1, TestWorker.class.getName());
				RedisBackingStore.persistJob(job, RedisBackingStore.SCHEDULED_JOBS, redis);

				String jobId = getJobId(job.getId());
				assertTrue(redis.exists(jobId));

				backingStore.removeJobById(job.getId(), redis);
				assertFalse(redis.exists(jobId));

				RedisBackingStore.releaseConnection(redis);
			}

		};
	}

	@Test
	public void removeCompletedJobById() {
		new JavaTestKit(system) {
			{
				Jedis redis = RedisBackingStore.getConnection();
				RedisBackingStore backingStore = new RedisBackingStore(system, settings);

				// Push a job into Redis
				Job job = new Job(1, TestWorker.class.getName());
				job.setCompletedAt(DateTime.now().minusYears(1));

				RedisBackingStore.persistJob(job, RedisBackingStore.COMPLETED_JOBS, redis);

				assertTrue(redis.exists(getJobId(job.getId())));

				// Remove the scheduled job
				backingStore.removeCompletedJobById(job.getId());

				assertFalse(redis.exists(getJobId(job.getId())));

				// Check scheduled list in Redis
				assertEquals("Expected no jobs in the completed jobs list", 0,
						redis.lrange(RedisBackingStore.COMPLETED_JOBS, 0, -1).size());

				RedisBackingStore.releaseConnection(redis);
			}
		};
	}

	@Test
	public void removeFailedJobById() {
		new JavaTestKit(system) {
			{
				Jedis redis = RedisBackingStore.getConnection();
				RedisBackingStore backingStore = new RedisBackingStore(system, settings);

				// Push a job into Redis
				Job job = new Job(1, TestWorker.class.getName());
				job.setCompletedAt(DateTime.now().minusYears(1));

				RedisBackingStore.persistJob(job, RedisBackingStore.FAILED_JOBS, redis);

				assertTrue(redis.exists(getJobId(job.getId())));

				// Remove the scheduled job
				backingStore.removeFailedJobById(job.getId());

				assertFalse(redis.exists(getJobId(job.getId())));

				// Check scheduled list in Redis
				assertEquals("Expected no jobs in the failed jobs list", 0,
						redis.lrange(RedisBackingStore.FAILED_JOBS, 0, -1).size());

				RedisBackingStore.releaseConnection(redis);
			}
		};
	}

	@Test
	public void removeScheduledJobById() {
		new JavaTestKit(system) {
			{
				Jedis redis = RedisBackingStore.getConnection();
				RedisBackingStore backingStore = new RedisBackingStore(system, settings);

				// Push a job into Redis
				Job job = new Job(1, TestWorker.class.getName());
				RedisBackingStore.persistJob(job, RedisBackingStore.SCHEDULED_JOBS, redis);

				assertTrue(redis.exists(getJobId(job.getId())));

				// Remove the scheduled job
				backingStore.removeScheduledJobById(job.getId());

				assertFalse(redis.exists(getJobId(job.getId())));

				// Check scheduled list in Redis
				assertEquals("Expected no jobs in the scheduled jobs list", 0,
						redis.lrange(RedisBackingStore.SCHEDULED_JOBS, 0, -1).size());

				RedisBackingStore.releaseConnection(redis);
			}
		};
	}

	@Test
	public void removeUnscheduledJobById() {
		new JavaTestKit(system) {
			{
				Jedis redis = RedisBackingStore.getConnection();
				RedisBackingStore backingStore = new RedisBackingStore(system, settings);

				// Push a job into Redis
				Job job = new Job(1, TestWorker.class.getName());
				RedisBackingStore.persistJob(job, RedisBackingStore.UNSCHEDULED_JOBS, redis);

				assertTrue(redis.exists(getJobId(job.getId())));

				// Remove the scheduled job
				backingStore.removeUnscheduledJobById(job.getId());

				assertFalse(redis.exists(getJobId(job.getId())));

				// Check scheduled list in Redis
				assertEquals("Expected no jobs in the unscheduled jobs list", 0,
						redis.lrange(RedisBackingStore.UNSCHEDULED_JOBS, 0, -1).size());

				RedisBackingStore.releaseConnection(redis);

			}
		};
	}

	private String getJobId(long id) {
		return String.format(JOB_KEY, id);
	}

}
