package oncue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import oncue.common.events.AgentStartedEvent;
import oncue.common.events.AgentStoppedEvent;
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
import akka.actor.UntypedActor;
import akka.event.EventStream;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class EventMachine extends UntypedActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	private static List<WebSocket.Out<JsonNode>> clients = new ArrayList<>();
	private final static ObjectMapper mapper = new ObjectMapper();

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
		log.info("EventMachine is listening for onCue events.");
	}

	public static void addSocket(WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out) {
		clients.add(out);
		in.onClose(new Callback0() {

			@Override
			public void invoke() throws Throwable {
				clients.remove(out);
			}
		});
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof AgentStartedEvent) {
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
				ObjectNode event = constructEvent("job:enqueued", "job", jobEnqueued.getJob());
				client.write(event);
			}
		} else if (message instanceof JobProgressEvent) {
			JobProgressEvent jobProgress = (JobProgressEvent) message;
			for (WebSocket.Out<JsonNode> client : clients) {
				ObjectNode event = constructEvent("job:progressed", "job", jobProgress.getJob());
				client.write(event);
			}
		} else if (message instanceof JobFailedEvent) {
			JobFailedEvent jobFailed = (JobFailedEvent) message;
			for (WebSocket.Out<JsonNode> client : clients) {
				ObjectNode event = constructEvent("job:failed", "job", jobFailed.getJob());
				client.write(event);
			}
		}
	}

	/**
	 * Construct an event
	 * 
	 * @param eventKey
	 *            is the composite event key, e.g. 'agent:started'
	 * @param subject
	 *            is the subject of the event, e.g. 'agent'
	 * @param payload
	 *            is the object to serialise
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
