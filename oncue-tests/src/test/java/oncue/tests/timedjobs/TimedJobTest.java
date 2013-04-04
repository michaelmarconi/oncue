package oncue.tests.timedjobs;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oncue.common.messages.Job;
import oncue.common.messages.RetryTimedJobMessage;
import oncue.common.messages.WorkResponse;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.TestWorker;
import oncue.timedjobs.TimedJobFactory;

import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.Kill;
import akka.camel.CamelMessage;
import akka.testkit.JavaTestKit;

public class TimedJobTest extends ActorSystemTest {

	public static final String CAMEL_WAIT_TIME = "30 seconds";

	@Test
	public void timedJobSendsJobMessage() {
		new JavaTestKit(system) {
			{
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

				// Create a timed job probe
				final JavaTestKit timedJobProbe = new JavaTestKit(system) {

					{
						new IgnoreMsg() {

							protected boolean ignore(Object message) {
								return !(message instanceof CamelMessage);
							}
						};
					}
				};

				// Create a queue manager
				createQueueManager(system, null);

				// Create a scheduler
				createScheduler(system, null);

				// Create an agent with a probe
				createAgent(system, Arrays.asList(TestWorker.class.getName()), agentProbe.getRef());

				// Expect a work response with no jobs
				WorkResponse response = agentProbe.expectMsgClass(duration("2 seconds"), WorkResponse.class);
				List<Job> jobs = response.getJobs();
				assertEquals(0, jobs.size());

				TimedJobFactory.createTimedJob(system, TestWorker.class.getName(), "test-1", "quartz://test-timer-1",
						null, timedJobProbe.getRef());
				TimedJobFactory.createTimedJob(system, TestWorker.class.getName(), "test-2", "quartz://test-timer-2",
						null, timedJobProbe.getRef());

				// Wait for Camel to start up
				timedJobProbe.expectMsgClass(duration(CAMEL_WAIT_TIME), CamelMessage.class);

				// Expect two workers to send work responses
				response = agentProbe.expectMsgClass(duration("4 seconds"), WorkResponse.class);
				assertEquals(TestWorker.class.getName(), response.getJobs().get(0).getWorkerType());

				response = agentProbe.expectMsgClass(duration("4 seconds"), WorkResponse.class);

				assertEquals(TestWorker.class.getName(), response.getJobs().get(0).getWorkerType());
			}
		};
	}

	@Test
	public void timedJobSendsJobMessageWithParameters() {
		new JavaTestKit(system) {
			{
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

				// Create a timed job probe
				final JavaTestKit timedJobProbe = new JavaTestKit(system) {

					{
						new IgnoreMsg() {

							protected boolean ignore(Object message) {
								return !(message instanceof CamelMessage);
							}
						};
					}
				};

				// Create a queue manager
				createQueueManager(system, null);

				// Create a scheduler
				createScheduler(system, null);

				// Create an agent with a probe
				createAgent(system, Arrays.asList(TestWorker.class.getName()), agentProbe.getRef());

				// Expect a work response with no jobs
				WorkResponse response = agentProbe.expectMsgClass(duration("2 seconds"), WorkResponse.class);
				List<Job> jobs = response.getJobs();
				assertEquals(0, jobs.size());

				Map<String, String> parameters = new HashMap<String, String>();
				parameters.put("key", "value");

				TimedJobFactory.createTimedJob(system, TestWorker.class.getName(), "test-1", "quartz://test-timer-1",
						parameters, timedJobProbe.getRef());

				// Wait for Camel to start up
				timedJobProbe.expectMsgClass(duration(CAMEL_WAIT_TIME), CamelMessage.class);

				// Expect two workers to send work responses
				response = agentProbe.expectMsgClass(duration("2 seconds"), WorkResponse.class);
				Job job = response.getJobs().get(0);
				assertEquals(TestWorker.class.getName(), job.getWorkerType());
				assertEquals(parameters, job.getParams());
			}
		};
	}

	@Test
	public void timedJobRetriesWhenQueueManagerIsNotFound() {
		new JavaTestKit(system) {
			{
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
				
				// Create a timed job probe
				final JavaTestKit timedJobProbe = new JavaTestKit(system) {

					{
						new IgnoreMsg() {

							protected boolean ignore(Object message) {
								return !(message instanceof RetryTimedJobMessage || message instanceof CamelMessage);
							}
						};
					}
				};

				// Create a scheduler
				createScheduler(system, null);

				// Create an agent with a probe
				createAgent(system, Arrays.asList(TestWorker.class.getName()), agentProbe.getRef());

				// Initial work response when agent starts
				WorkResponse response = agentProbe.expectMsgClass(duration("2 seconds"), WorkResponse.class);
				List<Job> jobs = response.getJobs();
				assertEquals(0, jobs.size());

				// Create timed job
				TimedJobFactory.createTimedJob(system, TestWorker.class.getName(), "test-1", "quartz://test-timer-1",
						null, timedJobProbe.getRef());

				// Wait for Camel to start up
				timedJobProbe.expectMsgClass(duration(CAMEL_WAIT_TIME), CamelMessage.class);

				agentProbe.expectNoMsg(duration("5 seconds"));

				// Create a queue manager
				createQueueManager(system, null);

				// Expect work response
				response = agentProbe.expectMsgClass(duration("5 seconds"), WorkResponse.class);
				Job job = response.getJobs().get(0);
				assertEquals(TestWorker.class.getName(), job.getWorkerType());
				assertEquals(null, job.getParams());
			}
		};
	}

	@Test
	public void timedJobRetriesSpecifiedNumberOfTimes() {
		new JavaTestKit(system) {
			{
				// Create an agent probe
				final JavaTestKit agentProbe = new JavaTestKit(system) {

					{
						new IgnoreMsg() {

							protected boolean ignore(Object message) {
								return !(message instanceof RetryTimedJobMessage || message instanceof CamelMessage);
							}
						};
					}
				};

				// Create a scheduler
				createScheduler(system, null);

				Map<String, String> params = new HashMap<String, String>();
				params.put("key", "value");

				// Create timed job
				int retryCount = 3;
				TimedJobFactory.createTimedJob(system, TestWorker.class.getName(), "test-1", "quartz://test-timer-1",
						params, retryCount, agentProbe.getRef());

				// Wait for Camel to start up
				agentProbe.expectMsgClass(duration(CAMEL_WAIT_TIME), CamelMessage.class);

				RetryTimedJobMessage timedJobMessage = agentProbe.expectMsgClass(duration("2 seconds"),
						RetryTimedJobMessage.class);
				validateRetryTimedJobMessageParams(params, TestWorker.class.getName(), timedJobMessage);
				timedJobMessage = agentProbe.expectMsgClass(duration("2 seconds"), RetryTimedJobMessage.class);
				validateRetryTimedJobMessageParams(params, TestWorker.class.getName(), timedJobMessage);
				timedJobMessage = agentProbe.expectMsgClass(duration("2 seconds"), RetryTimedJobMessage.class);
				validateRetryTimedJobMessageParams(params, TestWorker.class.getName(), timedJobMessage);

				agentProbe.expectNoMsg();
			}

		};
	}

	private static void validateRetryTimedJobMessageParams(Map<String, String> params, String workerType,
			RetryTimedJobMessage timedJobMessage) {
		assertEquals(workerType, timedJobMessage.getWorkerType());
		assertEquals(params, timedJobMessage.getJobParameters());
	}

	@Test
	public void timedJobGetsRestartedWhenKilled() {
		new JavaTestKit(system) {
			{
				// Create a timed job probe
				final JavaTestKit timedJobProbe = new JavaTestKit(system) {

					{
						new IgnoreMsg() {

							protected boolean ignore(Object message) {
								return !(message instanceof RetryTimedJobMessage || message instanceof CamelMessage);
							}
						};
					}
				};
				
				// Create a scheduler
				createScheduler(system, null);

				// Create timed job that uses the agent probe
				Map<String, String> params = new HashMap<String, String>();
				params.put("key", "value");

				String workerType = "oncue.workers.TestWorker";
				String actorPath = "quartz://test-timer-1";
				TimedJobFactory.createTimedJob(system, workerType, "test-1", actorPath, params, null,
						timedJobProbe.getRef());
				
				// Wait for Camel to start up
				timedJobProbe.expectMsgClass(duration(CAMEL_WAIT_TIME), CamelMessage.class);

				// Observe the timed job trying to schedule the job itself
				RetryTimedJobMessage timedJobMessage = timedJobProbe.expectMsgClass(duration("2 seconds"),
						RetryTimedJobMessage.class);
				validateRetryTimedJobMessageParams(params, workerType, timedJobMessage);

				// Kill the actor
				ActorRef timedJob = system.actorFor(actorPath);
				timedJob.tell(Kill.getInstance(), this.getRef());

				// Observe the timed job has restarted and is trying to schedule
				// the job itself
				timedJobMessage = timedJobProbe.expectMsgClass(duration("2 seconds"), RetryTimedJobMessage.class);
				validateRetryTimedJobMessageParams(params, workerType, timedJobMessage);

				timedJobMessage = timedJobProbe.expectMsgClass(duration("2 seconds"), RetryTimedJobMessage.class);
				validateRetryTimedJobMessageParams(params, workerType, timedJobMessage);

				expectNoMsg(duration("10 seconds"));
			}
		};
	}
}
