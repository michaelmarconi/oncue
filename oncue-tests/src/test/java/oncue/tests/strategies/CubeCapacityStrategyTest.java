package oncue.tests.strategies;

import static junit.framework.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import oncue.backingstore.BackingStore;
import oncue.common.messages.AbstractWorkRequest;
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.JobSummary;
import oncue.common.messages.SimpleMessages.SimpleMessage;
import oncue.common.messages.WorkResponse;
import oncue.scheduler.CubeCapacityScheduler;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.TestWorker;
import oncue.tests.workers.TestWorker2;

import org.junit.Test;

import scala.concurrent.duration.FiniteDuration;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;

import com.google.api.client.util.Maps;
import com.google.common.collect.Sets;

public class CubeCapacityStrategyTest extends ActorSystemTest {

	private static final String TEST_WORKER = TestWorker.class.getName();

	@Test
	public void doesNotScheduleJobsThatCannotBeHandledByTheAgent() {
		new JavaTestKit(system) {
			{
				// Create a scheduler
				ActorRef scheduler = createScheduler(system);

				// Enqueue jobs
				scheduler.tell(new EnqueueJob(TestWorker2.class.getName(), requiredMemory("2600")),
						getRef());
				expectMsgClass(Job.class);

				// ---

				// Create an agent that can run "TestWorker" workers
				final JavaTestKit agentProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {
							protected boolean ignore(Object message) {
								if (message instanceof WorkResponse)
									return false;
								else
									return true;
							}
						};
					}
				};

				createAgent(system, new HashSet<String>(Arrays.asList(TEST_WORKER)),
						agentProbe.getRef());

				// Expect empty work response
				WorkResponse workResponse = agentProbe.expectMsgClass(WorkResponse.class);
				assertEquals(0, workResponse.getJobs().size());
			}
		};
	}

	@Test
	public void doesNotScheduleJobsThatExceedCapacity() {
		new JavaTestKit(system) {
			{
				// Create a naked scheduler
				@SuppressWarnings("serial")
				final Props schedulerProps = new Props(new UntypedActorFactory() {
					@SuppressWarnings("unchecked")
					@Override
					public Actor create() throws ClassNotFoundException {
						return new CubeCapacityScheduler(
								(Class<? extends BackingStore>) Class
										.forName(settings.SCHEDULER_BACKING_STORE_CLASS));
					}
				});

				// Wait until the scheduler has three unscheduled jobs
				final TestActorRef<CubeCapacityScheduler> schedulerRef = TestActorRef.create(
						system, schedulerProps, settings.SCHEDULER_NAME);
				final CubeCapacityScheduler scheduler = schedulerRef.underlyingActor();
				scheduler.pause();

				// Enqueue jobs
				schedulerRef.tell(
						new EnqueueJob(TEST_WORKER, withParams(requiredMemory("2600"),
								processCode("foo1"))), getRef());
				expectMsgClass(Job.class);
				schedulerRef.tell(
						new EnqueueJob(TEST_WORKER, withParams(requiredMemory("2600"),
								processCode("foo2"))), getRef());
				expectMsgClass(Job.class);
				schedulerRef.tell(
						new EnqueueJob(TEST_WORKER, withParams(requiredMemory("1"),
								processCode("foo3"))), getRef());
				expectMsgClass(Job.class);

				new AwaitCond(duration("60 seconds"), duration("1 second")) {

					@Override
					protected boolean cond() {
						schedulerRef.tell(SimpleMessage.JOB_SUMMARY, getRef());
						JobSummary summary = expectMsgClass(JobSummary.class);
						return summary.getJobs().size() == 3;
					}
				};

				// Create an agent that can run "TestWorker" workers
				final JavaTestKit agentProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {
							protected boolean ignore(Object message) {
								if (message instanceof WorkResponse) {
									if (((WorkResponse) message).getJobs().isEmpty()) {
										return true;
									}
									return false;
								} else
									return true;
							}
						};
					}
				};

				scheduler.unpause();

				createAgent(system, Sets.newHashSet(TEST_WORKER), agentProbe.getRef());

				// // Expect three separate work responses
				WorkResponse workResponse = agentProbe.expectMsgClass(duration("5 seconds"),
						WorkResponse.class);
				assertEquals(2, workResponse.getJobs().size());
				assertEquals("2600", workResponse.getJobs().get(0).getParams().get("memory"));
				assertEquals(1, workResponse.getJobs().get(0).getId());
				assertEquals(3, workResponse.getJobs().get(1).getId());
				assertEquals("1", workResponse.getJobs().get(1).getParams().get("memory"));

				workResponse = agentProbe.expectMsgClass(duration("5 seconds"), WorkResponse.class);
				assertEquals(1, workResponse.getJobs().size());
				assertEquals(2, workResponse.getJobs().get(0).getId());
			}
		};
	}

	@Test
	public void memoryParameterOverridesConfigProvidedMemoryDefault() {
		assertEquals(
				500,
				config.getInt("oncue.scheduler.cube-capacity-scheduler.default-requirements.oncue.tests.workers.TestWorker2.memory"));
		new JavaTestKit(system) {
			{
				// Create an agent that can run "TestWorker" workers
				final JavaTestKit schedulerProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {
							protected boolean ignore(Object message) {
								if (message instanceof AbstractWorkRequest)
									return false;
								else
									return true;
							}
						};
					}
				};

				// Create a scheduler
				ActorRef scheduler = createScheduler(system, schedulerProbe.getRef());

				// Enqueue jobs
				scheduler.tell(new EnqueueJob(TestWorker.class.getName(), memory("200")), getRef());
				expectMsgClass(Job.class);

				// Create an agent that can run "TestWorker" workers
				final JavaTestKit agentProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {
							protected boolean ignore(Object message) {
								if (message instanceof WorkResponse)
									return false;
								else
									return true;
							}
						};
					}
				};

				createAgent(system, new HashSet<String>(Arrays.asList(TestWorker.class.getName())),
						agentProbe.getRef());

				// Expect a work response with only one job for that process
				WorkResponse workResponse = agentProbe.expectMsgClass(WorkResponse.class);
				assertEquals(1, workResponse.getJobs().size());
				assertEquals("200", workResponse.getJobs().get(0).getParams().get("memory"));
			}

		};
	}

	@Test
	public void doesNotScheduleMultipleJobsForTheSameMatchingProcess() {
		new JavaTestKit(system) {
			{
				// Create an agent that can run "TestWorker" workers
				final JavaTestKit schedulerProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {
							protected boolean ignore(Object message) {
								if (message instanceof AbstractWorkRequest)
									return false;
								else
									return true;
							}
						};
					}
				};

				// Create a scheduler
				ActorRef scheduler = createScheduler(system, schedulerProbe.getRef());

				// Enqueue jobs
				scheduler.tell(
						new EnqueueJob(TestWorker2.class.getName(), withParams(processCode("FOO"),
								memory("200"))), getRef());
				expectMsgClass(Job.class);
				scheduler.tell(
						new EnqueueJob(TestWorker2.class.getName(), withParams(processCode("FOO"),
								memory("200"))), getRef());
				expectMsgClass(Job.class);
				scheduler.tell(
						new EnqueueJob(TestWorker2.class.getName(), withParams(processCode("FOO2"),
								memory("200"))), getRef());
				expectMsgClass(Job.class);

				// ---

				// Create an agent that can run "TestWorker" workers
				final JavaTestKit agentProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {
							protected boolean ignore(Object message) {
								if (message instanceof WorkResponse)
									return false;
								else
									return true;
							}
						};
					}
				};

				createAgent(system,
						new HashSet<String>(Arrays.asList(TestWorker2.class.getName())),
						agentProbe.getRef());

				// Expect a work response with two jobs - one for each process
				WorkResponse workResponse = agentProbe.expectMsgClass(WorkResponse.class);
				assertEquals(2, workResponse.getJobs().size());
				assertEquals(1, workResponse.getJobs().get(0).getId());
				assertEquals(3, workResponse.getJobs().get(1).getId());

				// Expect worker to ask for more work
				schedulerProbe.expectMsgClass(new FiniteDuration(5, TimeUnit.SECONDS),
						AbstractWorkRequest.class);

				new AwaitCond() {
					@Override
					protected boolean cond() {
						// Expect a new work response after those jobs complete containing the
						// second worker for that. Uses an await condition because multiple work
						// responses could come back in the case of job 3 finishing before job 1.
						WorkResponse workResponse = agentProbe.expectMsgClass(WorkResponse.class);
						if (workResponse.getJobs().size() == 1
								&& workResponse.getJobs().get(0).getId() == 2) {
							return true;
						}

						return false;
					}
				};
			}

		};
	}

	@SafeVarargs
	private final Map<String, String> withParams(Map<String, String>... params) {
		Map<String, String> parameters = Maps.newHashMap();
		for (Map<String, String> param : params) {
			parameters.putAll(param);
		}
		return parameters;
	}

	private Map<String, String> memory(String memory) {
		Map<String, String> jobParams = Maps.newHashMap();
		jobParams.put("memory", memory);
		return jobParams;
	}

	private Map<String, String> processCode(String code) {
		Map<String, String> jobParams = Maps.newHashMap();
		jobParams.put("process_code", code);
		return jobParams;
	}

	private Map<String, String> requiredMemory(String memory) {
		Map<String, String> jobParams = Maps.newHashMap();
		jobParams.put("memory", memory);
		return jobParams;
	}

}
