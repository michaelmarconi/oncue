package oncue.tests.strategies;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

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
import oncue.scheduler.CapacityScheduler;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.load.workers.SimpleLoadTestWorker;
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

public class CapacityStrategyTest extends ActorSystemTest {

	private static final String TEST_WORKER = TestWorker.class.getName();

	@Test
	public void doesNotScheduleJobsThatCannotBeHandledByTheAgent() {
		new JavaTestKit(system) {
			{
				// Create a scheduler
				ActorRef scheduler = createScheduler(system);

				// Enqueue jobs
				scheduler.tell(new EnqueueJob(TestWorker2.class.getName(), memory("2600")),
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
						return new CapacityScheduler(
								(Class<? extends BackingStore>) Class
										.forName(settings.SCHEDULER_BACKING_STORE_CLASS));
					}
				});

				// Wait until the scheduler has three unscheduled jobs
				final TestActorRef<CapacityScheduler> schedulerRef = TestActorRef.create(system,
						schedulerProps, settings.SCHEDULER_NAME);
				final CapacityScheduler scheduler = schedulerRef.underlyingActor();
				scheduler.pause();

				// Enqueue jobs
				schedulerRef.tell(
						new EnqueueJob(TEST_WORKER, withParams(memory("2600"), code("foo1"))),
						getRef());
				expectMsgClass(Job.class);
				schedulerRef.tell(
						new EnqueueJob(TEST_WORKER, withParams(memory("2600"), code("foo2"))),
						getRef());
				expectMsgClass(Job.class);
				schedulerRef.tell(
						new EnqueueJob(TEST_WORKER, withParams(memory("1"), code("foo3"))),
						getRef());
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
				config.getInt("oncue.scheduler.capacity-scheduler.default-requirements.oncue.tests.workers.TestWorker2.memory"));
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
	public void doesNotScheduleMultipleJobsForTheSameConstrainedWorker() {
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
						new EnqueueJob(TestWorker2.class.getName(), withParams(code("FOO"),
								memory("200"))), getRef());
				expectMsgClass(Job.class);
				scheduler.tell(
						new EnqueueJob(TestWorker2.class.getName(), withParams(code("FOO"),
								memory("200"))), getRef());
				expectMsgClass(Job.class);
				scheduler.tell(
						new EnqueueJob(TestWorker2.class.getName(), withParams(code("FOO2"),
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

	@Test
	public void handlesMultipleWorkerTypesWithMultipleUniquenessKeysCorrectly() {
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
						new EnqueueJob(TestWorker2.class.getName(), withParams(code("FOO"),
								memory("200"))), getRef());
				expectMsgClass(Job.class);
				scheduler.tell(
						new EnqueueJob(TestWorker2.class.getName(), withParams(code("FOO"),
								memory("200"))), getRef());
				expectMsgClass(Job.class);
				scheduler.tell(
						new EnqueueJob(TestWorker2.class.getName(), withParams(code("FOO2"),
								memory("200"))), getRef());
				expectMsgClass(Job.class);
				scheduler.tell(
						new EnqueueJob(TestWorker.class.getName(), withParams(code("FOO2"),
								memory("200"), foo("foobar"))), getRef());
				scheduler.tell(
						new EnqueueJob(TestWorker.class.getName(), withParams(code("FOO2"),
								memory("200"), foo("bar"), bar("baz"))), getRef());
				scheduler.tell(
						new EnqueueJob(TestWorker.class.getName(), withParams(code("FOO2"),
								memory("200"), foo("bar"), bar("barbaz"))), getRef());
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

				createAgent(
						system,
						new HashSet<String>(Arrays.asList(TestWorker2.class.getName(),
								TestWorker.class.getName())), agentProbe.getRef());

				// Expect a work response with two TestWorker2 jobs - one for each process, and two
				// TestWorker1 classes - the first two have the different (code, foo) combinations
				WorkResponse workResponse = agentProbe.expectMsgClass(WorkResponse.class);
				assertEquals(4, workResponse.getJobs().size());
				assertEquals(1, workResponse.getJobs().get(0).getId());
				assertEquals(3, workResponse.getJobs().get(1).getId());
				assertEquals(4, workResponse.getJobs().get(2).getId());
				assertEquals(5, workResponse.getJobs().get(3).getId());

				new AwaitCond() {

					@Override
					protected boolean cond() {
						// Expect worker to ask for more work and get an empty response while it's
						// processing those jobs.
						WorkResponse workResponse = agentProbe.expectMsgClass(WorkResponse.class);
						if (workResponse.getJobs().size() == 0) {
							return false;
						}

						schedulerProbe.expectMsgClass(new FiniteDuration(5, TimeUnit.SECONDS),
								AbstractWorkRequest.class);

						// There's three possible scenarios here: the TestWorker and TestWorker2
						// jobs finish
						// at the same time and there's a work response with two new jobs, or one of
						// the
						// jobs finishes first and the other appears
						if (workResponse.getJobs().size() == 2) {
							assertEquals(2, workResponse.getJobs().get(0).getId());
							assertEquals(6, workResponse.getJobs().get(1).getId());
						} else {
							assertEquals(1, workResponse.getJobs().size());
							long id = workResponse.getJobs().get(0).getId();
							if (id != 0 && id != 6) {
								fail();
							}
							int expectedJobID = id == 6 ? 2 : 6;
							workResponse = agentProbe.expectMsgClass(WorkResponse.class);
							assertEquals(1, workResponse.getJobs().size());
							assertEquals(expectedJobID, workResponse.getJobs().get(0).getId());
						}

						return true;
					}
				};
			}

		};
	}

	@Test
	public void uniquenessConstrainedWorkerTypeWithNoConstrainedParametersUsesOnlyWorkerTypeAsUniquenessConstraint() {
		// i.e. if no parameters are defined, simply adding a worker type with no parameters will
		// enforce that only one of that worker type can happen at a time, ignoring the parameters
		// of any job with that worker type.
		new JavaTestKit(system) {
			{
				// Create an agent that can run "SimpleLoadTestWorker" workers
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
						new EnqueueJob(SimpleLoadTestWorker.class.getName(), withParams(
								code("FOO"), memory("200"))), getRef());
				expectMsgClass(Job.class);
				scheduler.tell(
						new EnqueueJob(SimpleLoadTestWorker.class.getName(), withParams(
								code("FOO2"), memory("200"))), getRef());
				expectMsgClass(Job.class);

				// ---

				// Create an agent that can run "SimpleLoadTestWorker" workers
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
						new HashSet<String>(Arrays.asList(SimpleLoadTestWorker.class.getName())),
						agentProbe.getRef());

				// Expect a work response with only one SimpleLoadTestWorker job
				WorkResponse workResponse = agentProbe.expectMsgClass(WorkResponse.class);
				assertEquals(1, workResponse.getJobs().size());
				assertEquals(1, workResponse.getJobs().get(0).getId());

				schedulerProbe.expectMsgClass(new FiniteDuration(5, TimeUnit.SECONDS),
						AbstractWorkRequest.class);

				new AwaitCond() {

					@Override
					protected boolean cond() {
						// Expect worker to ask for more work and get an empty response while it's
						// processing those jobs.
						WorkResponse workResponse = agentProbe.expectMsgClass(
								duration("5 seconds"), WorkResponse.class);
						if (workResponse.getJobs().size() == 0) {
							return false;
						}

						assertEquals(1, workResponse.getJobs().size());
						assertEquals(2, workResponse.getJobs().get(0).getId());

						return true;
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

	private Map<String, String> code(String code) {
		Map<String, String> jobParams = Maps.newHashMap();
		jobParams.put("code", code);
		return jobParams;
	}

	private Map<String, String> foo(String bar) {
		Map<String, String> jobParams = Maps.newHashMap();
		jobParams.put("foo", bar);
		return jobParams;
	}

	private Map<String, String> bar(String baz) {
		Map<String, String> jobParams = Maps.newHashMap();
		jobParams.put("bar", baz);
		return jobParams;
	}

}
