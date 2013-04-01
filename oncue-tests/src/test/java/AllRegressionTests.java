import oncue.tests.AgentPresenceTest;
import oncue.tests.AgentRegistrationTest;
import oncue.tests.AgentShutdownTest;
import oncue.tests.BroadcastWorkTest;
import oncue.tests.EnqueueJobTest;
import oncue.tests.JobProgressTest;
import oncue.tests.MissingWorkerTest;
import oncue.tests.WorkRequestTest;
import oncue.tests.WorkerTest;
import oncue.tests.redis.RedisBackingStoreTest;
import oncue.tests.redis.RedisQueueManagerTest;
import oncue.tests.robustness.AgentDiesTest;
import oncue.tests.robustness.RobustRedisDequeueTest;
import oncue.tests.robustness.SchedulerDiesTest;
import oncue.tests.robustness.WorkerDiesTest;
import oncue.tests.strategies.JVMCapacityStrategyTest;
import oncue.tests.timedjobs.TimedJobFactoryTest;
import oncue.tests.timedjobs.TimedJobTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * These regression tests exclude the load tests
 */
@RunWith(Suite.class)
@SuiteClasses({ 
		AgentPresenceTest.class, 
		AgentRegistrationTest.class, 
		AgentShutdownTest.class, 
		BroadcastWorkTest.class,
		EnqueueJobTest.class, 
		JobProgressTest.class, 
		MissingWorkerTest.class, 
		WorkerTest.class, 
		WorkRequestTest.class,
		
		RedisBackingStoreTest.class, 
		RedisQueueManagerTest.class, 
		
		AgentDiesTest.class, 
		RobustRedisDequeueTest.class,
		SchedulerDiesTest.class, 
		WorkerDiesTest.class, 
		
		JVMCapacityStrategyTest.class, 
		
		TimedJobFactoryTest.class,
		TimedJobTest.class })
public class AllRegressionTests {

}
