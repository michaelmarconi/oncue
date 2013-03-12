package oncue.timetable;

import java.util.Map;

import oncue.messages.internal.EnqueueJob;
import oncue.settings.Settings;
import oncue.settings.SettingsProvider;
import akka.actor.ActorRef;
import akka.camel.CamelMessage;
import akka.camel.javaapi.UntypedConsumerActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * A TimedJob is created from an entry in the timetable in the configuration file. It will enqueue
 * the specified job according to the specified Quartz schedule.
 * 
 * See http://camel.apache.org/quartz.html for URI specifications
 */
public class JobTimer extends UntypedConsumerActor {

	private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private Settings settings = SettingsProvider.SettingsProvider.get(getContext().system());

	private String workerType;

	private String schedule;
	private Map<String, String> params;

	public JobTimer(String workerType, String schedule, Map<String, String> params) {
		this.workerType = workerType;
		this.schedule = schedule;
		this.params = params;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof CamelMessage) {
			// Find the queue manager
			// TODO: This should access through the AkkaAPI, but we don't have a Config here
			ActorRef queueManager = this.getContext().actorFor(settings.QUEUE_MANAGER_PATH);

			EnqueueJob job = new EnqueueJob(workerType, params);
			queueManager.tell(job, getSelf());
		} else {
			unhandled(message);
		}
	}

	@Override
	public String getEndpointUri() {
		return schedule;
	}

}
