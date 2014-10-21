package oncue.tests.strategies;

import static junit.framework.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.WorkResponse;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.TestWorker;
import oncue.tests.workers.TestWorker2;

import org.junit.Test;

import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;

import com.google.api.client.util.Maps;

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
				// Create a scheduler
				ActorRef scheduler = createScheduler(system);

				// Enqueue jobs
				scheduler.tell(new EnqueueJob(TEST_WORKER, requiredMemory("2600")), getRef());
				expectMsgClass(Job.class);
				scheduler.tell(new EnqueueJob(TEST_WORKER, requiredMemory("2600")), getRef());
				expectMsgClass(Job.class);
				scheduler.tell(new EnqueueJob(TEST_WORKER, requiredMemory("1")), getRef());
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

				// TODO
				try {
					Thread.sleep(5000);
				} catch (Exception e) {
				}

				createAgent(system, new HashSet<String>(Arrays.asList(TEST_WORKER)),
						agentProbe.getRef());

				// Expect a work response with only job 1 at agent 1
				WorkResponse workResponse = agentProbe.expectMsgClass(WorkResponse.class);
				assertEquals(2, workResponse.getJobs().size());

				assertEquals("2600", workResponse.getJobs().get(0).getParams().get("memory"));
				assertEquals(1, workResponse.getJobs().get(0).getId());
				assertEquals(3, workResponse.getJobs().get(1).getId());
				assertEquals("1", workResponse.getJobs().get(1).getParams().get("memory"));
			}
		};
	}

	@Test
	public void doesNotScheduleMultipleJobsForTheSameMatchingProcess() {
		new JavaTestKit(system) {
			{
				// Create a scheduler
				ActorRef scheduler = createScheduler(system);

				// Enqueue jobs
				scheduler.tell(new EnqueueJob(TestWorker2.class.getName(), processCode("FOO")), getRef());
				expectMsgClass(Job.class);
				scheduler.tell(new EnqueueJob(TestWorker2.class.getName(), processCode("FOO")), getRef());
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

				// TODO
				try {
					Thread.sleep(5000);
				} catch (Exception e) {
				}

				createAgent(system, new HashSet<String>(Arrays.asList(TestWorker2.class.getName())),
						agentProbe.getRef());

				// Expect a work response with only job 1
				WorkResponse workResponse = agentProbe.expectMsgClass(WorkResponse.class);
				assertEquals(1, workResponse.getJobs().size());
				assertEquals(1, workResponse.getJobs().get(0).getId());
			}

		};
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
