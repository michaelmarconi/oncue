package oncue.timetable;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class TimedJobTest extends AbstractActorSystemTest {

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

				TimedJobFactory
						.createTimedJob(system, "oncue.workers.TestWorker", "test-1",
								"quartz://test-timer-1", null);
				TimedJobFactory
						.createTimedJob(system, "oncue.workers.TestWorker", "test-2",
								"quartz://test-timer-2", null);

				// Expect two workers to send work responses
				response = agentProbe.expectMsgClass(duration("3 seconds"), WorkResponse.class);
				assertEquals("oncue.workers.TestWorker", response.getJobs().get(0).getWorkerType());

				response = agentProbe.expectMsgClass(duration("3 seconds"), WorkResponse.class);
				assertEquals("oncue.workers.TestWorker", response.getJobs().get(0).getWorkerType());
			}
		};
	}
	
	@SuppressWarnings("serial")
	@Test
	public void jobTimerSendsJobMessageWithParameters() {
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

				Map<String, String> parameters = new HashMap<String, String>();
				parameters.put("key", "value");

				TimedJobFactory
						.createTimedJob(system, "oncue.workers.TestWorker", "test-1",
								"quartz://test-timer-1", parameters);

				// Expect two workers to send work responses
				response = agentProbe.expectMsgClass(duration("3 seconds"), WorkResponse.class);
				Job job = response.getJobs().get(0);
				assertEquals("oncue.workers.TestWorker", job.getWorkerType());
				assertEquals(parameters, job.getParams());
			}
		};
	}

	@SuppressWarnings("serial")
	@Test
	public void jobTimerRetriesWhenQueueManagerIsNotFound() {
		new JavaTestKit(system) {
			{
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

				// Initial work response when agent starts
				WorkResponse response = agentProbe.expectMsgClass(duration("3 seconds"),
						WorkResponse.class);
				List<Job> jobs = response.getJobs();
				assertEquals(0, jobs.size());

				// Create timed job
				TimedJobFactory
						.createTimedJob(system, "oncue.workers.TestWorker", "test-1",
								"quartz://test-timer-1", null);

				agentProbe.expectNoMsg(duration("5 seconds"));

				// Create a queue manager
				system.actorOf(new Props(InMemoryQueueManager.class), settings.QUEUE_MANAGER_NAME);

				// Expect work response
				response = agentProbe.expectMsgClass(duration("3 seconds"), WorkResponse.class);
				Job job = response.getJobs().get(0);
				assertEquals("oncue.workers.TestWorker", job.getWorkerType());
				assertEquals(null, job.getParams());
			}
		};
	}
}
