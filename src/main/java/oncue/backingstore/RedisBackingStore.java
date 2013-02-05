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
package oncue.backingstore;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oncue.backingstore.internal.AbstractBackingStore;
import oncue.messages.internal.Job;
import oncue.messages.internal.JobFailed;
import oncue.messages.internal.JobProgress;
import oncue.queueManager.RedisQueueManager;
import oncue.settings.Settings;

import org.joda.time.DateTime;
import org.json.simple.JSONValue;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Transaction;
import akka.actor.ActorSystem;

public class RedisBackingStore extends AbstractBackingStore {

	// The total count of persisted jobs
	public static final String JOB_COUNT_KEY = "oncue:job_count";

	// The time the job was enqueued
	public static final String JOB_ENQUEUED_AT = "job_enqueued_at";

	// The progress against a job
	public static final String JOB_FAILURE_STACKTRACE = "job_failure_stacktrace";

	// The ID of a job
	public static final String JOB_ID = "job_id";

	// The key to a particular job
	public static final String JOB_KEY = "oncue:jobs:%s";

	// The job parameters
	public static final String JOB_PARAMS = "job_params";

	// The progress against a job
	public static final String JOB_PROGRESS = "job_progress";

	// The job state
	public static final String JOB_STATE = "job_state";

	// The worker type assigned to a job
	public static final String JOB_WORKER_TYPE = "job_worker_type";

	/*
	 * The queue of jobs that acts as an external interface; the scheduler
	 * component will watch this queue for new jobs
	 */
	public static final String NEW_JOBS = "oncue:jobs:new";

	// A Redis connection pool
	private static JedisPool redisPool;

	// The scheduled jobs dispatched by the scheduler component
	public static final String SCHEDULED_JOBS = "oncue:jobs:scheduled";

	// The unscheduled jobs held by the scheduler
	public static final String UNSCHEDULED_JOBS = "oncue:jobs:unscheduled";

	/**
	 * Create a new {@linkplain Job} and persist it in Redis
	 * 
	 * @param workerType
	 *            is the type of worker required to complete this job
	 * 
	 * @param jobParams
	 *            is a map of String-based parameters
	 * 
	 * @return a new {@linkplain Job}
	 */
	public static Job createJob(String workerType, Map<String, String> jobParams) {

		// Get a connection to Redis
		Jedis redis = getConnection();

		// Get the latest job ID
		Long jobId = redis.incr(RedisBackingStore.JOB_COUNT_KEY);

		// Create a new job
		Job job = new Job(jobId, DateTime.now(), workerType, jobParams);

		// Now, persist the job and release the connection
		persistJob(job, RedisBackingStore.NEW_JOBS, redis);
		releaseConnection(redis);

		return job;
	}

	/**
	 * @return a {@linkplain Jedis} connection to Redis. Be sure to release this
	 *         when you're done with it!
	 */
	public static Jedis getConnection() {
		if (redisPool == null)
			// TODO use the config
			redisPool = new JedisPool(new JedisPoolConfig(), "localhost");
		return redisPool.getResource();
	}

	/**
	 * Construct a job from a given Job ID
	 * 
	 * @param id
	 *            is the id of the job
	 * @param redis
	 *            is a connection to Redis
	 * @return a {@linkplain Job} that represents the job hash in Redis
	 */
	@SuppressWarnings("unchecked")
	public static Job loadJob(long id, Jedis redis) {
		String jobKey = String.format(JOB_KEY, id);
		Job job;

		try {
			DateTime enqueuedAt = DateTime.parse(redis.hget(jobKey, JOB_ENQUEUED_AT));
			String workerType = redis.hget(jobKey, JOB_WORKER_TYPE);
			String progress = redis.hget(jobKey, JOB_PROGRESS);
			String params = redis.hget(jobKey, JOB_PARAMS);

			if (params == null)
				job = new Job(new Long(id), enqueuedAt, workerType);
			else
				job = new Job(new Long(id), enqueuedAt, workerType, (Map<String, String>) JSONValue.parse(params));

			if (progress != null)
				job.setProgress(new Double(progress));
		} catch (NullPointerException e) {
			throw new RuntimeException(String.format("Cannot find a job with id %s in Redis", id));
		}

		return job;
	}

	/**
	 * Persist a job as a hash in Redis
	 * 
	 * @param job
	 *            is the {@linkplain Job} to persist
	 * @param queueName
	 *            is the name of the queue to push the job onto
	 * @param redis
	 *            is a connection to Redis
	 */
	public static void persistJob(Job job, String queueName, Jedis redis) {

		// Persist the job in a transaction
		Transaction transaction = redis.multi();

		// Create a map describing the job
		String jobKey = String.format(JOB_KEY, job.getId());
		transaction.hset(jobKey, JOB_ENQUEUED_AT, job.getEnqueuedAt().toString());
		transaction.hset(jobKey, JOB_WORKER_TYPE, job.getWorkerType());

		if (job.getProgress() != null)
			transaction.hset(jobKey, JOB_PROGRESS, job.getProgress().toString());

		if (job.getParams() != null)
			transaction.hset(jobKey, JOB_PARAMS, JSONValue.toJSONString(job.getParams()));

		// Add the job to the specified queue
		transaction.lpush(queueName, new Long(job.getId()).toString());

		// Exec the transaction
		transaction.exec();
	}

	/**
	 * Release a Redis connection back into the pool
	 * 
	 * @param connection
	 *            is the Redis connection to release
	 */
	public static void releaseConnection(Jedis connection) {
		redisPool.returnResource(connection);
	}

	public RedisBackingStore(ActorSystem system, Settings settings) {
		super(system, settings);
	}

	@Override
	public void addScheduledJobs(List<Job> jobs) {
		Jedis redis = RedisBackingStore.getConnection();

		for (Job job : jobs) {
			redis.lpush(SCHEDULED_JOBS, new Long(job.getId()).toString());
		}

		RedisBackingStore.releaseConnection(redis);
	}

	@Override
	public void addUnscheduledJob(Job job) {
		Jedis redis = RedisBackingStore.getConnection();

		// Save the unscheduled job if it doesn't exist
		String jobKey = String.format(JOB_KEY, job.getId());
		if (!redis.exists(jobKey))
			persistJob(job, UNSCHEDULED_JOBS, redis);
		else {
			/*
			 * A Redis-backed queue manager will have pushed the job onto the
			 * "unscheduled" queue atomically already, so don't add it again!
			 */
			if (!settings.QUEUE_MANAGER_CLASS.equals(RedisQueueManager.class.getName())) {
				redis.lpush(UNSCHEDULED_JOBS, new Long(job.getId()).toString());
			}
		}

		RedisBackingStore.releaseConnection(redis);
	}

	@Override
	public void persistJobFailure(JobFailed jobFailed) {
		Job job = jobFailed.getJob();

		// Flatten the stack trace
		StringWriter stackTraceWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stackTraceWriter);
		jobFailed.getError().printStackTrace(printWriter);

		Jedis redis = getConnection();

		Transaction transaction = redis.multi();

		String jobKey = String.format(JOB_KEY, job.getId());
		transaction.hset(jobKey, JOB_STATE, Job.State.FAILED.toString());
		transaction.hset(jobKey, JOB_FAILURE_STACKTRACE, stackTraceWriter.toString());

		transaction.exec();

		releaseConnection(redis);
	}

	@Override
	public void persistJobProgress(JobProgress jobProgress) {
		Jedis redis = getConnection();

		Job job = jobProgress.getJob();
		String jobKey = String.format(JOB_KEY, job.getId());
		redis.hset(jobKey, RedisBackingStore.JOB_PROGRESS, new Double(jobProgress.getProgress()).toString());

		releaseConnection(redis);
	}

	@Override
	public long popUnscheduledJob() {
		Jedis redis = RedisBackingStore.getConnection();

		String jobID = redis.rpop(UNSCHEDULED_JOBS);

		RedisBackingStore.releaseConnection(redis);

		return new Long(jobID);
	}

	@Override
	public void removeScheduledJob(Job job) {
		Jedis redis = RedisBackingStore.getConnection();

		redis.lrem(SCHEDULED_JOBS, 0, new Long(job.getId()).toString());

		RedisBackingStore.releaseConnection(redis);
	}

	@Override
	public void removeUnscheduledJob(Job job) {
		Jedis redis = RedisBackingStore.getConnection();

		redis.lrem(UNSCHEDULED_JOBS, 0, new Long(job.getId()).toString());

		RedisBackingStore.releaseConnection(redis);
	}

	/**
	 * When restoring the jobs queue, we need to look for all the jobs that were
	 * on the scheduler jobs queue in Redis, as well as the jobs that had been
	 * scheduled against agents, which we assume are dead.
	 */
	@Override
	public List<Job> restoreJobs() {
		List<Job> jobs = new ArrayList<Job>();

		Jedis redis = RedisBackingStore.getConnection();

		/*
		 * Pop all scheduled jobs back onto the unscheduled jobs queue
		 */
		while (redis.rpoplpush(SCHEDULED_JOBS, UNSCHEDULED_JOBS) != null) {
		}

		// Get all the unscheduled jobs
		List<String> jobIDs = redis.lrange(UNSCHEDULED_JOBS, 0, -1);
		for (String jobID : jobIDs) {
			jobs.add(loadJob(new Long(jobID), redis));
		}

		RedisBackingStore.releaseConnection(redis);

		return jobs;
	}
}
