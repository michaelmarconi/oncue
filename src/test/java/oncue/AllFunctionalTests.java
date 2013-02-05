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
package oncue;


import oncue.functional.AgentPresenceTest;
import oncue.functional.AgentRegistrationTest;
import oncue.functional.AgentShutdownTest;
import oncue.functional.BroadcastWorkTest;
import oncue.functional.EnqueueJobTest;
import oncue.functional.JobProgressTest;
import oncue.functional.MissingWorkerTest;
import oncue.functional.WorkRequestTest;
import oncue.functional.WorkerTest;
import oncue.functional.redis.RedisBackingStoreTest;
import oncue.functional.redis.RedisQueueManagerTest;
import oncue.functional.robustness.AgentDiesTest;
import oncue.functional.robustness.RobustRedisDequeueTest;
import oncue.functional.robustness.SchedulerDiesTest;
import oncue.functional.robustness.WorkerDiesTest;
import oncue.functional.strategies.JVMCapacityStrategyTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

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
	
	WorkerDiesTest.class,
	AgentDiesTest.class,
	SchedulerDiesTest.class,
	RobustRedisDequeueTest.class,
	
	JVMCapacityStrategyTest.class,
	
// TODO Re-enable API tests, which are flickering!
//	AkkaAPITests.class,
//	RedisAPITests.class
})
public class AllFunctionalTests {

}
