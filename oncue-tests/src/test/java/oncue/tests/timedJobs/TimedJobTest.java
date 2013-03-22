package oncue.tests.timedJobs;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oncue.agent.UnlimitedCapacityAgent;
import oncue.common.messages.Job;
import oncue.common.messages.RetryTimedJobMessage;
import oncue.common.messages.WorkResponse;
import oncue.service.queuemanager.InMemoryQueueManager;
import oncue.service.scheduler.SimpleQueuePopScheduler;
import oncue.service.timedjobs.TimedJobFactory;
import oncue.tests.base.AbstractActorSystemTest;
import oncue.tests.workers.TestWorker;

import org.junit.Test;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Kill;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;

public class TimedJobTest extends AbstractActorSystemTest {

	@SuppressWarnings("serial")
	@Test
	public void timedJobSendsJobMessage() {
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

				TimedJobFactory.createTimedJob(system, "oncue.workers.TestWorker", "test-1",
						"quartz://test-timer-1", null);
				TimedJobFactory.createTimedJob(system, "oncue.workers.TestWorker", "test-2",
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
	public void timedJobSendsJobMessageWithParameters() {
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
	public void timedJobRetriesWhenQueueManagerIsNotFound() {
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

	@SuppressWarnings("serial")
	@Test
	public void timedJobRetriesSpecifiedNumberOfTimes() {
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
								return !(message instanceof RetryTimedJobMessage);
							}
						};
					}
				};

				Map<String, String> params = new HashMap<String, String>();
				params.put("key", "value");

				// Create timed job
				int retryCount = 3;
				String workerType = "oncue.workers.TestWorker";
				TimedJobFactory.createTimedJob(system, workerType, "test-1",
						"quartz://test-timer-1", params, retryCount, agentProbe.getRef());

				RetryTimedJobMessage timedJobMessage = agentProbe.expectMsgClass(
						duration("3 seconds"), RetryTimedJobMessage.class);
				validateRetryTimedJobMessageParams(params, workerType, timedJobMessage);
				timedJobMessage = agentProbe.expectMsgClass(duration("3 seconds"),
						RetryTimedJobMessage.class);
				validateRetryTimedJobMessageParams(params, workerType, timedJobMessage);
				timedJobMessage = agentProbe.expectMsgClass(duration("3 seconds"),
						RetryTimedJobMessage.class);
				validateRetryTimedJobMessageParams(params, workerType, timedJobMessage);
				
				agentProbe.expectNoMsg();
			}

		};
	}

	private static void validateRetryTimedJobMessageParams(Map<String, String> params,
			String workerType, RetryTimedJobMessage timedJobMessage) {
		assertEquals(workerType, timedJobMessage.getWorkerType());
		assertEquals(params, timedJobMessage.getJobParameters());
	}

	@SuppressWarnings("serial")
	@Test
	public void timedJobGetsRestartedWhenKilled() {
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
								return !(message instanceof RetryTimedJobMessage);
							}
						};
					}
				};

				// Create timed job that uses the agent probe
				Map<String, String> params = new HashMap<String, String>();
				params.put("key", "value");

				String workerType = "oncue.workers.TestWorker";
				String actorPath = "quartz://test-timer-1";
				TimedJobFactory.createTimedJob(system, workerType, "test-1", actorPath, params, null,
						agentProbe.getRef());

				// Observe the timed job trying to schedule the job itself
				RetryTimedJobMessage timedJobMessage = agentProbe.expectMsgClass(
						duration("3 seconds"), RetryTimedJobMessage.class);
				validateRetryTimedJobMessageParams(params, workerType, timedJobMessage);

				// Kill the actor
				ActorRef timedJob = system.actorFor(actorPath);
				timedJob.tell(Kill.getInstance(), this.getRef());

				// Observe the timed job has restarted and is trying to schedule the job itself
				timedJobMessage = agentProbe.expectMsgClass(duration("3 seconds"),
						RetryTimedJobMessage.class);
				validateRetryTimedJobMessageParams(params, workerType, timedJobMessage);

				timedJobMessage = agentProbe.expectMsgClass(duration("3 seconds"),
						RetryTimedJobMessage.class);
				validateRetryTimedJobMessageParams(params, workerType, timedJobMessage);

				expectNoMsg(duration("10 seconds"));
			}
		};
	}
}
