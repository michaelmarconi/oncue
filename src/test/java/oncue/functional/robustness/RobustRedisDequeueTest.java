package oncue.functional.robustness;

import static junit.framework.Assert.assertEquals;
import oncue.backingstore.RedisBackingStore;
import oncue.base.AbstractActorSystemTest;
import oncue.messages.internal.EnqueueJob;
import oncue.messages.internal.Job;
import oncue.queueManager.RedisQueueManager;
import oncue.scheduler.SimpleQueuePopScheduler;
import oncue.workers.TestWorker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;

/**
 * When we are running the Redis Queue Manager, we atomically pop and push a new
 * job into the Unscheduled jobs queue.
 * 
 * Further, we need to ensure that a Redis Backing Store doesn't attempt to push
 * a newly discovered job onto the Unscheduled jobs queue, since it is there
 * already!
 */
public class RobustRedisDequeueTest extends AbstractActorSystemTest {

	private Jedis redis;

	@Test
	@SuppressWarnings("serial")
	public void atomicPopAndPush() {
		new JavaTestKit(system) {
			{
				// Create a queue manager probe
				final JavaTestKit queueManagerProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {
							@Override
							protected boolean ignore(Object message) {
								return !(message instanceof Job);
							}
						};
					}
				};

				// Create a Redis queue manager with a probe
				ActorRef queueManager = system.actorOf(new Props(new UntypedActorFactory() {
					@Override
					public Actor create() throws Exception {
						RedisQueueManager queueManager = new RedisQueueManager();
						queueManager.injectProbe(queueManagerProbe.getRef());
						return queueManager;
					}
				}), settings.QUEUE_MANAGER_NAME);

				// Enqueue a job
				queueManager.tell(new EnqueueJob(TestWorker.class.getName()), null);

				// Expect the job at the queue manager
				Job job = queueManagerProbe.expectMsgClass(duration("5 seconds"), Job.class);

				// Ensure the job is no longer on the New jobs queue
				assertEquals(0, redis.llen(RedisBackingStore.NEW_JOBS).longValue());

				// Ensure the job is already on the Unscheduled jobs queue
				assertEquals(1, redis.llen(RedisBackingStore.UNSCHEDULED_JOBS).longValue());
				String jobId = redis.rpop(RedisBackingStore.UNSCHEDULED_JOBS);
				assertEquals(job.getId(), new Long(jobId).longValue());
			}
		};
	}

	@Test
	@SuppressWarnings("serial")
	public void bypassAddingUnscheduledJob() {
		new JavaTestKit(system) {
			{
				// Create a Redis queue manager
				ActorRef queueManager = system.actorOf(new Props(RedisQueueManager.class), settings.QUEUE_MANAGER_NAME);

				// Create a Redis-backed scheduler with a probe
				system.actorOf(new Props(new UntypedActorFactory() {
					@Override
					public Actor create() throws Exception {
						SimpleQueuePopScheduler scheduler = new SimpleQueuePopScheduler(RedisBackingStore.class);
						scheduler.injectProbe(getRef());
						return scheduler;
					}
				}), settings.SCHEDULER_NAME);

				// Enqueue a job
				queueManager.tell(new EnqueueJob(TestWorker.class.getName()), null);

				// Expect the job at the scheduler
				expectMsgClass(duration("5 seconds"), Job.class);

				// Give the scheduler a chance to persist the unscheduled job
				expectNoMsg(duration("1 second"));

				// Ensure the job is on Unscheduled job queue once
				long jobCount = redis.llen(RedisBackingStore.UNSCHEDULED_JOBS);

				assertEquals(1, jobCount);
			}
		};
	}

	@Before
	public void getRedisConnection() {
		redis = RedisBackingStore.getConnection();
	}

	@After
	public void releaseRedisConnection() {
		redis.flushAll();
		RedisBackingStore.releaseConnection(redis);
	}

}
