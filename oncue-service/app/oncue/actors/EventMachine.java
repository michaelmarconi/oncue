package oncue.actors;

import java.util.ArrayList;
import java.util.List;

import oncue.common.events.AgentStarted;
import oncue.common.events.AgentStopped;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import play.libs.F.Callback0;
import play.libs.Json;
import play.mvc.WebSocket;
import akka.actor.UntypedActor;
import akka.event.EventStream;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class EventMachine extends UntypedActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(),
			this);
	private static List<WebSocket.Out<JsonNode>> clients = new ArrayList<>();

	@Override
	public void preStart() {
		super.preStart();
		EventStream eventStream =  getContext().system().eventStream();
		eventStream.subscribe(getSelf(), AgentStarted.class);
		eventStream.subscribe(getSelf(), AgentStopped.class);
		log.info("EventMachine is listening for onCue events.");
	}

	public static void addSocket(WebSocket.In<JsonNode> in,
			final WebSocket.Out<JsonNode> out) {
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
		System.err.println(message);
		
		if (message instanceof AgentStarted) {
			AgentStarted agentStarted = (AgentStarted) message;
			for (WebSocket.Out<JsonNode> client : clients) {
				ObjectNode event = constructEvent("agent:started", "agent",
						agentStarted.getAgent());
				client.write(event);
			}
		}

		else if (message instanceof AgentStopped) {
			AgentStopped agentStopped = (AgentStopped) message;
			for (WebSocket.Out<JsonNode> client : clients) {
				ObjectNode event = constructEvent("agent:stopped", "agent",
						agentStopped.getAgent());
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
	private ObjectNode constructEvent(String eventKey, String subject,
			Object payload) {
		ObjectNode eventNode = Json.newObject();
		ObjectNode payloadNode = Json.newObject();
		eventNode.put(eventKey, payloadNode);
		payloadNode.put(subject, Json.toJson(payload));
		return eventNode;
	}

}
