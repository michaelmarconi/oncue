package oncue.timetable;

import static akka.pattern.Patterns.ask;
import static java.lang.String.format;

import java.util.Map;

import oncue.messages.internal.EnqueueJob;
import oncue.settings.Settings;
import oncue.settings.SettingsProvider;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;
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

	private Map<String, String> params;

	public TimedJob(String workerType, String schedule, Map<String, String> params) {
		this.workerType = workerType;
		this.schedule = schedule;
		this.params = params;
	}

	@Override
	public void onReceive(Object message) {
		if (message instanceof CamelMessage) {
			this.log.debug("Received Camel message for timed job submission for worker type {}", this.workerType);

			boolean success = attemptToEnqueuedJob();
			while (!success) {
				// Attempt to reschedule the job after the API timeout has elapsed
				success = attemptToEnqueuedJob();
			}

			this.log.debug("WOO");
		} else {
			unhandled(message);
		}
	}

	/**
	 * Attempt to enqueue a job with the job parameters and worker type in the Queue Manager.
	 * 
	 * @return true on success false on failure
	 */
	private boolean attemptToEnqueuedJob() {
		try {
			EnqueueJob enqueueJobMessage = new EnqueueJob(this.workerType, this.params);

			Await.result(
					ask(getContext().actorFor(this.settings.QUEUE_MANAGER_PATH),
							enqueueJobMessage, new Timeout(this.settings.API_TIMEOUT)),
					this.settings.API_TIMEOUT);

			return true;
		} catch (Exception e) {
			this.log.error(e, 
					format("Failed to enqueue timed job for worker type %s in queue manager",
							this.workerType));
			return false;
		}
	}

	@Override
	public String getEndpointUri() {
		return this.schedule;
	}

}
