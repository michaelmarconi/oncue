package actors;

import java.util.ArrayList;
import java.util.List;

import oncue.common.events.AgentAvailable;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import play.libs.F.Callback0;
import play.libs.Json;
import play.mvc.WebSocket;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class EventStreamListener extends UntypedActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	private static List<WebSocket.Out<JsonNode>> clients = new ArrayList<>();

	@Override
	public void preStart() {
		super.preStart();
		log.info("EventStreamListener is running");
		getContext().system().eventStream().subscribe(getSelf(), AgentAvailable.class);
	}

	public static void addClient(WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out) {
		clients.add(out);
		System.out.println("Added websocket client!");
		in.onClose(new Callback0() {

			@Override
			public void invoke() throws Throwable {
				clients.remove(out);
				System.out.println("Removed websocket client!");
			}
		});
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof AgentAvailable) {
			AgentAvailable agentAvailable = (AgentAvailable) message;
			for (WebSocket.Out<JsonNode> client : clients) {
				ObjectNode event = Json.newObject();
				event.put("agent", agentAvailable.getAgent());
				client.write(event);
			}
		}
	}

}
