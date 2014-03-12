package oncue.tests;

import static akka.pattern.Patterns.ask;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;

import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.JobProgress;
import oncue.common.messages.JobSummary;
import oncue.common.messages.SimpleMessages.SimpleMessage;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.TestWorker;

import org.junit.Test;

import scala.concurrent.Await;
import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;
import akka.util.Timeout;

/**
 * A series of tests to ensure that a job goes through the proper state
 * transitions as it is queued, scheduled, run, etc.
 */
public class JobStateTest extends ActorSystemTest {

	/**
	 * Check to see that a job transitions through all states as expected
	 */
	@Test
	public void testJobTransitionToCompletion() {
		new JavaTestKit(system) {
			{
				// Create a scheduler probe
				final JavaTestKit schedulerProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {
							@Override
							protected boolean ignore(Object message) {
								return !(message instanceof JobProgress);
							}
						};
					}
				};

				// Create a scheduler with a probe
				ActorRef scheduler = createScheduler(system, schedulerProbe.getRef());

				// Enqueue a job
				scheduler.tell(new EnqueueJob(TestWorker.class.getName()), getRef());

				// Expect a queued job from the scheduler
				Job job = expectMsgClass(Job.class);
				assertEquals("The job has the state", Job.State.QUEUED, job.getState());

				// Start an agent
				createAgent(system, new HashSet<String>(Arrays.asList(TestWorker.class.getName())));

				// Scheduler progress
				JobProgress schedulerProgress = schedulerProbe.expectMsgClass(JobProgress.class);
				assertEquals("Was expecting the job to be running.", Job.State.RUNNING, schedulerProgress.getJob()
						.getState());

				try {
					// Ask the Scheduler about its jobs
					JobSummary jobSummary = (JobSummary) Await.result(
							ask(scheduler, SimpleMessage.JOB_SUMMARY, new Timeout(settings.SCHEDULER_TIMEOUT)),
							settings.SCHEDULER_TIMEOUT);

					assertEquals(1, jobSummary.getJobs().size());
					assertEquals("The job should be in the 'running' state", Job.State.RUNNING, jobSummary.getJobs()
							.get(0).getState());

				} catch (Exception e) {
					fail("Timeout waiting for response from scheduler");
				}

				// Wait for job to complete at Scheduler
				for (int i = 0; i < 3; i++) {
					schedulerProgress = schedulerProbe.expectMsgClass(JobProgress.class);
					assertEquals("Was expecting the job to be running.", Job.State.RUNNING, schedulerProgress.getJob()
							.getState());
				}
				schedulerProgress = schedulerProbe.expectMsgClass(JobProgress.class);
				assertEquals("Was expecting the job to be complete.", Job.State.COMPLETE, schedulerProgress.getJob()
						.getState());

				try {
					// Ask the Scheduler about its jobs
					JobSummary jobSummary = (JobSummary) Await.result(
							ask(scheduler, SimpleMessage.JOB_SUMMARY, new Timeout(settings.SCHEDULER_TIMEOUT)),
							settings.SCHEDULER_TIMEOUT);

					assertEquals(1, jobSummary.getJobs().size());
					assertEquals("The job should be in the 'complete' state", Job.State.COMPLETE, jobSummary.getJobs()
							.get(0).getState());

				} catch (Exception e) {
					fail("Timeout waiting for response from scheduler");
				}
			}
		};
	}
}
