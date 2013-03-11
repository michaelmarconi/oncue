package oncue.timetable;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import oncue.agent.UnlimitedCapacityAgent;
import oncue.base.AbstractActorSystemTest;
import oncue.messages.internal.Job;
import oncue.messages.internal.WorkResponse;
import oncue.queueManager.InMemoryQueueManager;
import oncue.scheduler.SimpleQueuePopScheduler;
import oncue.workers.TestWorker;

import org.junit.Test;

import akka.actor.Actor;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;

public class JobTimerTest extends AbstractActorSystemTest {

	@SuppressWarnings("serial")
	@Test
	public void jobTimerSendsJobMessage() {
		new JavaTestKit(system) {

			{
				// Create a queue manager
				system.actorOf(new Props(InMemoryQueueManager.class), settings.QUEUE_MANAGER_NAME);

				// Create a simple scheduler
				system.actorOf(new Props(new UntypedActorFactory() {

					@Override
					public Actor create() throws Exception {
						return new SimpleQueuePopScheduler(null);
					}
				}), settings.SCHEDULER_NAME);

				// Create an agent probe
				final JavaTestKit agentProbe = new JavaTestKit(system) {

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

				// Create an agent
				system.actorOf(new Props(new UntypedActorFactory() {

					@Override
					public Actor create() throws Exception {
						UnlimitedCapacityAgent agent = new UnlimitedCapacityAgent(Arrays
								.asList(TestWorker.class.getName()));
						agent.injectProbe(agentProbe.getRef());
						return agent;
					}
				}), settings.AGENT_NAME);

				WorkResponse response = agentProbe.expectMsgClass(duration("3 seconds"),
						WorkResponse.class);
				List<Job> jobs = response.getJobs();
				assertEquals(0, jobs.size());

				JobTimerFactory
						.createTimedJob(system, "oncue.workers.TestWorker", "test-job-1",
								"quartz://test-worker-1?trigger.repeatInterval=1000&trigger.repeatCount=1&fireNow=false");

//				expectNoMsg(duration("30 seconds"));
				// TODO: Assert that the message is of the type we want

				response = agentProbe.expectMsgClass(duration("3 seconds"), WorkResponse.class);
				response.getJobs().get(0).getWorkerType();
			}
		};
	}
}
