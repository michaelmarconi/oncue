package oncue.timetable;

import static akka.pattern.Patterns.ask;
import static java.lang.String.format;
import static scala.concurrent.Await.result;

import java.util.Map;

import oncue.messages.internal.EnqueueJob;
import oncue.messages.internal.RetryTimedJobMessage;
import oncue.settings.Settings;
import oncue.settings.SettingsProvider;
import akka.actor.ActorRef;
import akka.camel.CamelMessage;
import akka.camel.javaapi.UntypedConsumerActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.Timeout;

/**
 * A TimedJob is created from an entry in the timetable in the configuration file. It will enqueue
 * the specified job according to the specified Quartz schedule.
 * 
 * If the job cannot be queued due to communications errors, it will wait for the API timeout period
 * defined in the settings before trying again. Currently it will try forever.
 * 
 * See http://camel.apache.org/quartz.html for URI specifications
 */
public class TimedJob extends UntypedConsumerActor {

	private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private Settings settings = SettingsProvider.SettingsProvider.get(getContext().system());

	private String workerType;

	private String schedule;

	private Integer failureRetryCount;

	private Map<String, String> params;

	// An optional probe for testing
	protected ActorRef testProbe;

	public TimedJob(String workerType, String schedule, Map<String, String> params,
			Integer failureRetryCount, ActorRef testProbe) {
		this.workerType = workerType;
		this.schedule = schedule;
		this.params = params;
		this.testProbe = testProbe;
		this.failureRetryCount = failureRetryCount;
	}

	public TimedJob(String workerType, String schedule, Map<String, String> params) {
		this(workerType, schedule, params, null, null);
	}

	@Override
	public void onReceive(final Object message) throws TimedJobException {
		if (testProbe != null) {
			testProbe.tell(message, getSelf());
		}

		if (message instanceof CamelMessage) {
			log.debug("Received Camel message for timed job submission for worker type {}",
					workerType);

			tryEnqueueJob(workerType, params);
		} else if (message instanceof RetryTimedJobMessage) {
			log.info("Retrying timed job submission for worker type {}", workerType);
			RetryTimedJobMessage retryMessage = (RetryTimedJobMessage) message;
			tryEnqueueJob(retryMessage.getWorkerType(), retryMessage.getJobParameters());
		} else {
			unhandled(message);
		}
	}

	/*
	 * Attempt to enqueue a job with the queue manager. If any exceptions are caught they will be
	 * logged, then the job will be rescheduled to run in the future.
	 * 
	 * @param workerType The qualified class name of the worker to instantiate
	 * 
	 * @param jobParameters The user-defined parameters map to pass to the job
	 */
	private void tryEnqueueJob(String workerType, Map<String, String> jobParameters)
			throws TimedJobException {
		try {
			enqueueJob(workerType, jobParameters);
		} catch (Exception e) {
			log.info("Failed to enqueue timed job for worker type {}", workerType);

			if (failureRetryCount == null) {
				sendRetryMessage(workerType, jobParameters);
			} else {
				if (failureRetryCount > 0) {
					failureRetryCount -= 1;
					sendRetryMessage(workerType, jobParameters);
				} else {
					throw new TimedJobException(
							format("Failed to enqueue job for worker type '%s' after specified number of retries.",
									workerType));
				}
			}
		}
	}

	private void sendRetryMessage(String workerType, Map<String, String> jobParameters) {
		RetryTimedJobMessage retryMessage = new RetryTimedJobMessage(workerType, jobParameters);
		getContext()
				.system()
				.scheduler()
				.scheduleOnce(settings.API_TIMEOUT, getSelf(), retryMessage,
						getContext().dispatcher());
	}

	/**
	 * Submit the job to the specified queue manager.
	 * 
	 * @param workerType The qualified class name of the worker to instantiate
	 * @param jobParameters The user-defined parameters map to pass to the job
	 * @throws Exception If the queue manager does not exist or the job is not accepted within the
	 * timeout
	 */
	private void enqueueJob(String workerType, Map<String, String> jobParameters) throws Exception {
		result(ask(getContext().actorFor(settings.QUEUE_MANAGER_PATH), new EnqueueJob(workerType,
				jobParameters), new Timeout(settings.API_TIMEOUT)), settings.API_TIMEOUT);
	}

	@Override
	public String getEndpointUri() {
		return schedule;
	}

}
