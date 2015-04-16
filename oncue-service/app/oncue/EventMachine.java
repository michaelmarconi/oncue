package oncue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import oncue.common.events.AgentStartedEvent;
import oncue.common.events.AgentStoppedEvent;
import oncue.common.events.JobCleanupEvent;
import oncue.common.events.JobEnqueuedEvent;
import oncue.common.events.JobFailedEvent;
import oncue.common.events.JobProgressEvent;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.PropertyNamingStrategy;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.node.ObjectNode;

import play.libs.F.Callback0;
import play.libs.Json;
import play.mvc.WebSocket;
import scala.concurrent.duration.Duration;
import akka.actor.Cancellable;
import akka.actor.UntypedActor;
import akka.event.EventStream;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class EventMachine extends UntypedActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	private static List<WebSocket.Out<JsonNode>> clients = new ArrayList<>();
	private final static ObjectMapper mapper = new ObjectMapper();
	private final Cancellable pinger = getContext()
			.system()
			.scheduler()
			.schedule(Duration.create(500, TimeUnit.MILLISECONDS),
					Duration.create(30000, TimeUnit.MILLISECONDS), getSelf(), "PING",
					getContext().dispatcher());

	static {
		mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz"));
		mapper.configure(SerializationConfig.Feature.WRITE_ENUMS_USING_TO_STRING, true);
		mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
	}

	@Override
	public void preStart() {
		super.preStart();
		EventStream eventStream = getContext().system().eventStream();
		eventStream.subscribe(getSelf(), AgentStartedEvent.class);
		eventStream.subscribe(getSelf(), AgentStoppedEvent.class);
		eventStream.subscribe(getSelf(), JobEnqueuedEvent.class);
		eventStream.subscribe(getSelf(), JobProgressEvent.class);
		eventStream.subscribe(getSelf(), JobFailedEvent.class);
		eventStream.subscribe(getSelf(), JobCleanupEvent.class);
		log.info("EventMachine is listening for OnCue events.");
	}

	@Override
	public void postStop() {
		pinger.cancel();
	}

	public static void addSocket(WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out) {
		clients.add(out);
		in.onClose(new Callback0() {

			@Override
			public void invoke() {
				clients.remove(out);
			}
		});
	}

	@Override
	public void onReceive(Object message) {
		if ("PING".equals(message)) {
			log.debug("Pinging websocket clients...");
			for (WebSocket.Out<JsonNode> client : clients) {
				client.write(Json.toJson("PING"));
			}
		} else if (message instanceof AgentStartedEvent) {
			AgentStartedEvent agentStarted = (AgentStartedEvent) message;
			for (WebSocket.Out<JsonNode> client : clients) {
				ObjectNode event = constructEvent("agent:started", "agent", agentStarted.getAgent());
				client.write(event);
			}
		} else if (message instanceof AgentStoppedEvent) {
			AgentStoppedEvent agentStopped = (AgentStoppedEvent) message;
			for (WebSocket.Out<JsonNode> client : clients) {
				ObjectNode event = constructEvent("agent:stopped", "agent", agentStopped.getAgent());
				client.write(event);
			}
		} else if (message instanceof JobEnqueuedEvent) {
			JobEnqueuedEvent jobEnqueued = (JobEnqueuedEvent) message;
			for (WebSocket.Out<JsonNode> client : clients) {
				ObjectNode event = constructEvent("job:enqueued", "job", jobEnqueued.getJob().clonePublicView());
				client.write(event);
			}
		} else if (message instanceof JobProgressEvent) {
			JobProgressEvent jobProgress = (JobProgressEvent) message;
			for (WebSocket.Out<JsonNode> client : clients) {
				ObjectNode event = constructEvent("job:progressed", "job", jobProgress.getJob().clonePublicView());
				client.write(event);
			}
		} else if (message instanceof JobFailedEvent) {
			JobFailedEvent jobFailed = (JobFailedEvent) message;
			for (WebSocket.Out<JsonNode> client : clients) {
				ObjectNode event = constructEvent("job:failed", "job", jobFailed.getJob().clonePublicView());
				client.write(event);
			}
		} else if (message instanceof JobCleanupEvent) {
			for (WebSocket.Out<JsonNode> client : clients) {
				ObjectNode event = constructEvent("jobs:cleanup", "jobs", null);
				client.write(event);
			}
		}
	}

	/**
	 * Construct an event
	 * 
	 * @param eventKey is the composite event key, e.g. 'agent:started'
	 * @param subject is the subject of the event, e.g. 'agent'
	 * @param payload is the object to serialise
	 * @return a JSON object node representing the event
	 */
	private ObjectNode constructEvent(String eventKey, String subject, Object payload) {
		ObjectNode eventNode = Json.newObject();
		ObjectNode payloadNode = Json.newObject();
		eventNode.put(eventKey, payloadNode);
		payloadNode.put(subject, mapper.valueToTree(payload));
		return eventNode;
	}

}
