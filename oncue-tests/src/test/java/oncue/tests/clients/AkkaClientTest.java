package oncue.tests.clients;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import oncue.client.AkkaClient;
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.tests.base.ActorSystemTest;
import oncue.tests.workers.TestWorker;

import org.junit.Test;

import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;

public class AkkaClientTest extends ActorSystemTest {

	@Test
	public void enqueueJobWithWorkerTypeAndMapOfParamsEnqueuesAJob()
			throws Exception {
		new JavaTestKit(system) {
			{

				final JavaTestKit schedulerProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {
							protected boolean ignore(Object message) {
								if (message instanceof EnqueueJob)
									return false;
								else
									return true;
							}
						};
					}
				};

				ActorRef scheduler = createScheduler(system,
						schedulerProbe.getRef());

				final String workerType = "oncue.tests.workers.TestWorker";
				final Map<String, String> jobParams = new HashMap<>();
				jobParams.put("A", "B");

				AkkaClient client = new AkkaClient(system, scheduler);
				client.enqueueJob(workerType, jobParams);

				EnqueueJob job = schedulerProbe
						.expectMsgClass(EnqueueJob.class);

				assertEquals(workerType, job.getWorkerType());
				assertEquals(jobParams, job.getParams());
			}

		};
	}

	@Test
	public void enqueueJobWithWorkerTypeEnqueuesAJob() throws Exception {
		new JavaTestKit(system) {
			{

				final JavaTestKit schedulerProbe = new JavaTestKit(system) {
					{
						new IgnoreMsg() {
							protected boolean ignore(Object message) {
								if (message instanceof EnqueueJob)
									return false;
								else
									return true;
							}
						};
					}
				};

				ActorRef scheduler = createScheduler(system,
						schedulerProbe.getRef());

				final String workerType = "oncue.tests.workers.TestWorker";

				AkkaClient client = new AkkaClient(system, scheduler);
				client.enqueueJob(workerType);

				EnqueueJob job = schedulerProbe
						.expectMsgClass(EnqueueJob.class);

				assertEquals(workerType, job.getWorkerType());
				assertEquals(new HashMap<String, String>(), job.getParams());
			}

		};
	}

	@Test
	public void getJobsReturnsAllJobsKnownToTheScheduler() throws Exception {
		new JavaTestKit(system) {
			{

				ActorRef scheduler = createScheduler(system);

				final Map<String, String> job1Params = new HashMap<>();
				job1Params.put("A", "B");

				scheduler.tell(new EnqueueJob(TestWorker.class.getName(),
						job1Params), getRef());
				Job expectedJob = expectMsgClass(Job.class);

				AkkaClient client = new AkkaClient(system, scheduler);

				Collection<Job> jobs = client.getJobs();
				assertNotNull(jobs);
				assertEquals(1, jobs.size());

				Iterator<Job> jobsIterator = jobs.iterator();
				Job actualJob = jobsIterator.next();

				assertEquals(expectedJob.getWorkerType(),
						actualJob.getWorkerType());
				assertEquals(expectedJob.getParams(), actualJob.getParams());
			}

		};
	}

}
