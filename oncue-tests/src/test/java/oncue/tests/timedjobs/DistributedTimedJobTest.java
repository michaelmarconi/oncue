package oncue.tests.timedjobs;

import java.util.Collections;

import oncue.tests.base.DistributedActorSystemTest;
import oncue.tests.load.workers.SimpleLoadTestWorker;

import org.junit.Test;

import akka.testkit.JavaTestKit;

/**
 * @MF: go ahead and sort out this implementation. Ensure you're using a
 *      Throttled Scheduler + Agent if you're loading it up, as the unlimited
 *      strategy may behave weirdly with no constraints!
 */
public class DistributedTimedJobTest extends DistributedActorSystemTest {

	@Test
	public void timedJobsWorkWithDistributedAgents() {

		// Create a scheduler probe
		final JavaTestKit schedulerProbe = new JavaTestKit(serviceSystem) {
			{
				new IgnoreMsg() {
					protected boolean ignore(Object message) {
						return true;
					}
				};
			}
		};

		// Create a queue manager
		createQueueManager(null);

		// Create a scheduler with a probe
		createScheduler(schedulerProbe.getRef());

		// Create a throttled agent
		createAgent(Collections.singletonList(SimpleLoadTestWorker.class.getName()), null);

		schedulerProbe.expectNoMsg();
	}

}
