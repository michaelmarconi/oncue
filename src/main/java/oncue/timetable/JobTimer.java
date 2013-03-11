package oncue.timetable;

import oncue.messages.internal.EnqueueJob;
import oncue.settings.Settings;
import oncue.settings.SettingsProvider;
import akka.actor.ActorRef;
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

	public JobTimer(String workerType, String schedule) {
		this.workerType = workerType;
		this.schedule = schedule;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		log.debug(message.toString());

		// Find the queue manager
		ActorRef queueManager = this.getContext().actorFor(settings.QUEUE_MANAGER_PATH);

		// TODO: Jobs with parameters
		EnqueueJob msg = new EnqueueJob(workerType);
		queueManager.tell(msg, getSelf());
	}

	@Override
	public String getEndpointUri() {
		return schedule;
	}

}
