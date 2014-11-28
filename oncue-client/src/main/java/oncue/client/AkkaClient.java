package oncue.client;

import static akka.pattern.Patterns.ask;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.JobSummary;
import oncue.common.messages.SimpleMessages.SimpleMessage;
import oncue.common.settings.Settings;
import oncue.common.settings.SettingsProvider;
import scala.concurrent.Await;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.AskTimeoutException;
import akka.util.Timeout;

public class AkkaClient implements Client {

	private LoggingAdapter log;

	private Settings settings;

	private ActorRef scheduler;

	public AkkaClient(ActorSystem system, ActorRef scheduler) {
		this.scheduler = scheduler;
		log = Logging.getLogger(system, this);
		settings = SettingsProvider.SettingsProvider.get(system);
	}

	@Override
	public Job enqueueJob(String workerType) throws ClientException {
		return enqueueJob(workerType, new HashMap<String, String>());
	}

	@Override
	public Job enqueueJob(String workerType, Map<String, String> jobParams)
			throws ClientException {
		try {
			return (Job) Await.result(
					ask(scheduler, new EnqueueJob(workerType, jobParams),
							new Timeout(settings.SCHEDULER_TIMEOUT)),
					settings.SCHEDULER_TIMEOUT);
		} catch (Exception e) {
			if (e instanceof AskTimeoutException) {
				log.error(e, "Timeout waiting for scheduler to enqueue job");
			} else {
				log.error(e, "Failed to enqueue job");
			}

			throw new ClientException(e);
		}
	}

	@Override
	public Collection<Job> getJobs() throws ClientException {
		try {
			JobSummary jobSummary = (JobSummary) Await.result(
					ask(scheduler, SimpleMessage.JOB_SUMMARY, new Timeout(
							settings.SCHEDULER_TIMEOUT)), settings.SCHEDULER_TIMEOUT);
			return jobSummary.getJobs();
		} catch (AskTimeoutException e) {
			log.error(e, "Timeout waiting for scheduler to respond with job summary");
			throw new ClientException(e);
		} catch (Exception e) {
			log.error(e, "Failed to retrieve job summary");
			throw new ClientException(e);
		}
	}

}
