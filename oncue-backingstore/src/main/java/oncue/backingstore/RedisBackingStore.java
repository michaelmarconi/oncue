/*******************************************************************************
 * Copyright 2013 Michael Marconi
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package oncue.backingstore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.json.simple.JSONValue;

import com.typesafe.config.Config;

import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import oncue.common.messages.Job;
import oncue.common.messages.Job.State;
import oncue.common.settings.Settings;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisException;

public class RedisBackingStore extends AbstractBackingStore {

	/**
	 * An AutoCloseable wrapper that manages fetching a connection from the redis connection pool
	 * and safely returning it when finished.
	 * 
	 * This class proxies methods from Jedis for convenience.
	 */
	public static class RedisConnection implements AutoCloseable {

		// A Redis connection pool
		private static JedisPool redisPool;

		private Jedis connection;

		public RedisConnection() {
			if (redisPool == null) {
				redisPool = new JedisPool(new JedisPoolConfig(), host, port,
						Protocol.DEFAULT_TIMEOUT, null);
			}
			this.connection = redisPool.getResource();
		}

		@Override
		public void close() {
			this.connection.close();
		}

		public Long incr(String key) {
			return this.connection.incr(key);
		}

		public Transaction multi() {
			return this.connection.multi();
		}

		public void lpush(String key, String value) {
			this.connection.lpush(key, value);
		}

		public List<String> lrange(String key, int start, int end) {
			return this.connection.lrange(key, start, end);
		}

		public String hget(String key, String field) {
			return this.connection.hget(key, field);
		}

		public void hset(String key, String field, String value) {
			this.connection.hset(key, field, value);
		}

		public void lrem(String key, int count, String value) {
			this.connection.lrem(key, count, value);
		}

		public void del(String key) {
			this.connection.del(key);
		}

		public Object rpoplpush(String srckey, String dstkey) {
			return this.connection.rpoplpush(srckey, dstkey);
		}

		/**
		 * Flush the entire redis database. This should only be used in tests.
		 */
		public void flushDB() {
			this.connection.flushDB();
		}

		public boolean exists(String key) {
			return this.connection.exists(key);
		}

		public Long llen(String key) {
			return this.connection.llen(key);
		}

		public List<String> brpop(int timeout, String key) {
			return this.connection.brpop(timeout, key);
		}
	}

	// Redis host config key
	private static final String REDIS_HOST = "oncue.scheduler.backing-store.redis.host";

	// Redis port config key
	private static final String REDIS_PORT = "oncue.scheduler.backing-store.redis.port";

	// Redis port
	private static int port = Protocol.DEFAULT_PORT;

	// The jobs that have completed successfully
	public static final String COMPLETED_JOBS = "oncue:jobs:complete";

	// The jobs that have failed
	public static final String FAILED_JOBS = "oncue:jobs:failed";

	// Redis host
	private static String host = "localhost";

	// The total count of persisted jobs
	public static final String JOB_COUNT_KEY = "oncue:job_count";

	// The time the job was enqueued
	public static final String JOB_ENQUEUED_AT = "job_enqueued_at";

	// The time the job was started
	public static final String JOB_STARTED_AT = "job_started_at";

	// The time the job was completed
	public static final String JOB_COMPLETED_AT = "job_completed_at";

	// The message associated with a failed job
	public static final String JOB_ERROR_MESSAGE = "job_failure_message";

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

	// The re-run status of a job
	public static final String JOB_RERUN_STATUS = "job_rerun_status";

	/*
	 * The queue of jobs that acts as an external interface; the scheduler component will watch this
	 * queue for new jobs
	 */
	public static final String NEW_JOBS = "oncue:jobs:new";

	// The scheduled jobs dispatched by the scheduler component
	public static final String SCHEDULED_JOBS = "oncue:jobs:scheduled";

	// The unscheduled jobs held by the scheduler
	public static final String UNSCHEDULED_JOBS = "oncue:jobs:unscheduled";

	/**
	 * Create a new {@linkplain Job} and persist it in Redis
	 * 
	 * @param workerType is the type of worker required to complete this job
	 * 
	 * @param params is a map of job parameters
	 * 
	 * @return a new {@linkplain Job}
	 */
	public static Job createJob(String workerType, Map<String, String> params) {
		try (RedisConnection redis = new RedisConnection()) {
			// Get the latest job ID
			Long jobId = redis.incr(RedisBackingStore.JOB_COUNT_KEY);

			// Create a new job
			Job job = new Job(jobId, workerType);
			if (params != null)
				job.setParams(params);

			// Now, persist the job and release the connection
			persistJob(job, RedisBackingStore.NEW_JOBS, redis);
			return job;
		}
	}

	/**
	 * Construct a job from a given Job ID
	 * 
	 * @param id is the id of the job
	 * @param redis is a connection to Redis
	 * @return a {@linkplain Job} that represents the job hash in Redis
	 */
	@SuppressWarnings("unchecked")
	public static Job loadJob(long id, RedisConnection redis) {
		String jobKey = String.format(JOB_KEY, id);
		Job job;

		try {
			DateTime enqueuedAt = DateTime.parse(redis.hget(jobKey, JOB_ENQUEUED_AT));

			DateTime startedAt = null;
			String startedAtRaw = redis.hget(jobKey, JOB_STARTED_AT);
			if (startedAtRaw != null)
				startedAt = DateTime.parse(startedAtRaw);

			DateTime completedAt = null;
			String completedAtRaw = redis.hget(jobKey, JOB_COMPLETED_AT);
			if (completedAtRaw != null)
				completedAt = DateTime.parse(completedAtRaw);

			String workerType = redis.hget(jobKey, JOB_WORKER_TYPE);
			String state = redis.hget(jobKey, JOB_STATE);
			String progress = redis.hget(jobKey, JOB_PROGRESS);
			String params = redis.hget(jobKey, JOB_PARAMS);
			String errorMessage = redis.hget(jobKey, JOB_ERROR_MESSAGE);
			String rerunStatus = redis.hget(jobKey, JOB_RERUN_STATUS);

			job = new Job(new Long(id), workerType);
			job.setEnqueuedAt(enqueuedAt);

			if (startedAt != null)
				job.setStartedAt(startedAt);

			if (completedAt != null)
				job.setCompletedAt(completedAt);

			job.setRerun(Boolean.parseBoolean(rerunStatus));

			if (params != null)
				job.setParams((Map<String, String>) JSONValue.parse(params));

			if (state != null)
				job.setState(State.valueOf(state.toUpperCase()));

			if (progress != null)
				job.setProgress(new Double(progress));

			if (errorMessage != null)
				job.setErrorMessage(errorMessage);

		} catch (Exception e) {
			throw new RuntimeException(
					String.format("Could not load job with id %s from Redis", id), e);
		}

		return job;
	}

	/**
	 * Persist a job as a hash in Redis
	 * 
	 * @param job is the {@linkplain Job} to persist
	 * @param queueName is the name of the queue to push the job onto
	 * @param redis is a connection to Redis
	 */
	public static void persistJob(Job job, String queueName, RedisConnection redis) {
		// Persist the job in a transaction
		try (Transaction transaction = redis.multi()) {

			// Create a map describing the job
			String jobKey = String.format(JOB_KEY, job.getId());
			transaction.hset(jobKey, JOB_ENQUEUED_AT, job.getEnqueuedAt().toString());

			if (job.getStartedAt() != null)
				transaction.hset(jobKey, JOB_STARTED_AT, job.getStartedAt().toString());

			if (job.getCompletedAt() != null)
				transaction.hset(jobKey, JOB_COMPLETED_AT, job.getCompletedAt().toString());

			transaction.hset(jobKey, JOB_WORKER_TYPE, job.getWorkerType());
			transaction.hset(jobKey, JOB_RERUN_STATUS, Boolean.toString(job.isRerun()));

			if (job.getParams() != null) {
				Map<String, String> params = null;
				switch (job.getState()) {
				case COMPLETE:
				case FAILED:
					params = job.getParams(false);
					break;
				default:
					params = job.getParams();
					break;
				}
				transaction.hset(jobKey, JOB_PARAMS, JSONValue.toJSONString(params));
			}

			if (job.getState() != null)
				transaction.hset(jobKey, JOB_STATE, job.getState().toString());

			transaction.hset(jobKey, JOB_PROGRESS, String.valueOf(job.getProgress()));

			if (job.getErrorMessage() != null)
				transaction.hset(jobKey, JOB_ERROR_MESSAGE, job.getErrorMessage());

			// Add the job to the specified queue
			transaction.lpush(queueName, Long.toString(job.getId()));

			// Exec the transaction
			transaction.exec();
		} catch (IOException e) {
			// Jedis' Transaction.close() method does not actually throw IOException, it just says
			// that it does. In fact it can only throw a JedisConnectionException, an instance of a
			// RuntimeException. Let's wrap this in a JedisException anyway to be sure.
			throw new JedisException(e);
		}
	}

	// Logger
	private LoggingAdapter log;

	public RedisBackingStore(ActorSystem system, Settings settings) {
		super(system, settings);

		/*
		 * Override Redis hostname and port from configuration
		 */
		Config config = system.settings().config();
		if (config.hasPath(REDIS_HOST)) {
			host = config.getString(REDIS_HOST);
		}
		if (config.hasPath(REDIS_PORT)) {
			port = config.getInt(REDIS_PORT);
		}

		log = Logging.getLogger(system, this);
		log.info("Backing store expects Redis at: host={}, port={}", host, port);
	}

	@Override
	public void addScheduledJobs(List<Job> jobs) {
		try (RedisConnection redis = new RedisConnection()) {
			for (Job job : jobs) {
				redis.lpush(SCHEDULED_JOBS, Long.toString(job.getId()));
			}
		}
	}

	@Override
	public void addUnscheduledJob(Job job) {
		try (RedisConnection redis = new RedisConnection()) {
			persistJob(job, UNSCHEDULED_JOBS, redis);
		}
	}

	@Override
	public List<Job> getCompletedJobs() {
		List<Job> jobs = new ArrayList<>();

		try (RedisConnection redis = new RedisConnection()) {
			List<String> jobIDs = redis.lrange(COMPLETED_JOBS, 0, -1);
			for (String jobID : jobIDs) {
				jobs.add(loadJob(new Long(jobID), redis));
			}
		}
		return jobs;
	}

	@Override
	public List<Job> getFailedJobs() {
		List<Job> jobs = new ArrayList<>();
		try (RedisConnection redis = new RedisConnection()) {
			List<String> jobIDs = redis.lrange(FAILED_JOBS, 0, -1);
			for (String jobID : jobIDs) {
				jobs.add(loadJob(new Long(jobID), redis));
			}
		}

		return jobs;
	}

	@Override
	public long getNextJobID() {
		try (RedisConnection redis = new RedisConnection()) {
			// Increment and return the latest job ID
			return redis.incr(RedisBackingStore.JOB_COUNT_KEY);
		}
	}

	@Override
	public void persistJobFailure(Job job) {
		try (RedisConnection redis = new RedisConnection()) {
			persistJob(job, FAILED_JOBS, redis);
		}
	}

	@Override
	public void persistJobProgress(Job job) {
		try (RedisConnection redis = new RedisConnection()) {
			String jobKey = String.format(JOB_KEY, job.getId());
			redis.hset(jobKey, JOB_PROGRESS, String.valueOf(job.getProgress()));
			redis.hset(jobKey, JOB_STATE, job.getState().toString());
			if (job.getStartedAt() != null)
				redis.hset(jobKey, JOB_STARTED_AT, job.getStartedAt().toString());

			if (job.getState() == Job.State.COMPLETE) {
				if (job.getCompletedAt() != null)
					redis.hset(jobKey, JOB_COMPLETED_AT, job.getCompletedAt().toString());
				redis.lpush(COMPLETED_JOBS, Long.toString(job.getId()));
			}
		}
	}

	@Override
	public void removeCompletedJobById(long jobId) {
		try (RedisConnection redis = new RedisConnection()) {
			redis.lrem(COMPLETED_JOBS, 0, Long.toString(jobId));
			removeJobById(jobId, redis);
		}
	}

	@Override
	public void removeFailedJobById(long jobId) {
		try (RedisConnection redis = new RedisConnection()) {
			redis.lrem(FAILED_JOBS, 0, Long.toString(jobId));
			removeJobById(jobId, redis);
		}
	}

	@Override
	public void removeScheduledJobById(long jobId) {
		try (RedisConnection redis = new RedisConnection()) {
			redis.lrem(SCHEDULED_JOBS, 0, Long.toString(jobId));
		}
	}

	@Override
	public void removeUnscheduledJobById(long jobId) {
		try (RedisConnection redis = new RedisConnection()) {
			redis.lrem(UNSCHEDULED_JOBS, 0, Long.toString(jobId));
		}
	}

	public void removeJobById(long jobId, RedisConnection redis) {
		redis.del(String.format(JOB_KEY, jobId));
	}

	/**
	 * When restoring the jobs queue, we need to look for all the jobs that were on the scheduler
	 * jobs queue in Redis, as well as the jobs that had been scheduled against agents, which we
	 * assume are dead.
	 */
	@Override
	public List<Job> restoreJobs() {
		List<Job> jobs = new ArrayList<>();

		try (RedisConnection redis = new RedisConnection()) {
			// Pop all scheduled jobs back onto the unscheduled jobs queue
			while (redis.rpoplpush(SCHEDULED_JOBS, UNSCHEDULED_JOBS) != null) {
			}

			// Get all the unscheduled jobs
			List<String> jobIDs = redis.lrange(UNSCHEDULED_JOBS, 0, -1);
			for (String jobID : jobIDs) {
				jobs.add(loadJob(new Long(jobID), redis));
			}
		}

		return jobs;
	}

	@Override
	public int cleanupJobs(boolean includeFailedJobs, Duration expirationAge) {
		int cleanedJobsCount = 0;

		for (Job completedJob : getCompletedJobs()) {
			DateTime expirationThreshold = DateTime.now().minus(expirationAge.getMillis());
			boolean isExpired = completedJob.getCompletedAt()
					.isBefore(expirationThreshold.toInstant());
			if (isExpired) {
				removeCompletedJobById(completedJob.getId());
				cleanedJobsCount++;
			}
		}

		if (!includeFailedJobs) {
			return cleanedJobsCount;
		}

		for (Job failedJob : getFailedJobs()) {
			if (failedJob.getCompletedAt() == null) {
				log.error(
						"Found a failed job with no completion time.  Setting completion time to now and defering to next clean up. ("
								+ failedJob.toString() + ")");
				failedJob.setCompletedAt(DateTime.now());
				persistJobFailure(failedJob);
				continue;
			}
			DateTime expirationThreshold = DateTime.now().minus(expirationAge.getMillis());
			boolean isExpired = failedJob.getCompletedAt()
					.isBefore(expirationThreshold.toInstant());
			if (isExpired) {
				removeFailedJobById(failedJob.getId());
				cleanedJobsCount++;
			}
		}

		return cleanedJobsCount;
	}
}
