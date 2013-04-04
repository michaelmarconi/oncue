package oncue.tests.timedjobs;

import java.util.Collections;

import junit.framework.Assert;
import oncue.backingstore.RedisBackingStore;
import oncue.common.messages.Job;
import oncue.common.messages.WorkResponse;
import oncue.tests.base.DistributedActorSystemTest;
import oncue.tests.load.workers.SimpleLoadTestWorker;
import oncue.timedjobs.TimedJobFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import akka.testkit.JavaTestKit;

public class DistributedTimedJobTest extends DistributedActorSystemTest {

	@After
	@Before
	public void flushRedis() {
		Jedis redis = RedisBackingStore.getConnection();
		redis.flushDB();
		RedisBackingStore.releaseConnection(redis);
	}

	@Test
	public void timedJobsWorkWithDistributedAgents() {
		final JavaTestKit agentProbe = new JavaTestKit(agentSystem) {

			{
				new IgnoreMsg() {

					protected boolean ignore(Object message) {
						if (message instanceof WorkResponse) {
							return false;
						}

						return true;
					}
				};
			}
		};

		createQueueManager(null);
		createScheduler(null);

		// Create a throttled agent
		createAgent(Collections.singletonList(SimpleLoadTestWorker.class.getName()),
				agentProbe.getRef());

		// The agent should connect to the scheduler and get an empty work response
		agentProbe.expectMsgClass(WorkResponse.class);

		// Initialise timed jobs
		TimedJobFactory.createTimedJobs(serviceSystem, serviceSettings.TIMED_JOBS_TIMETABLE);

		// Wait until all the jobs have completed
		final Jedis redis = RedisBackingStore.getConnection();

		final int JOB_COUNT = serviceConfig.getInt("oncue.timed-jobs.repeatCount") + 1;

		new JavaTestKit(serviceSystem) {
			{
				new AwaitCond(duration("5 minutes"), duration("10 seconds")) {

					@Override
					protected boolean cond() {
						Job finalJob;
						try {
							finalJob = RedisBackingStore.loadJob(JOB_COUNT, redis);
							return finalJob.getProgress() == 1.0;
						} catch (RuntimeException e) {
							// Job may not exist in Redis yet
							return false;
						}
					}
				};
			}
		};

		// Now, check all the jobs completed in Redis
		for (int i = 0; i < JOB_COUNT; i++) {
			Job job = RedisBackingStore.loadJob(i + 1, redis);
			Assert.assertEquals(1.0, job.getProgress());
		}

		serviceLog.info("All jobs were processed!");

		RedisBackingStore.releaseConnection(redis);
	}
}
