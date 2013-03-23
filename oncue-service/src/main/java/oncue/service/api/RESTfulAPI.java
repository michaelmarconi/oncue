package oncue.service.api;

import akka.camel.CamelMessage;
import akka.camel.javaapi.UntypedConsumerActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class RESTfulAPI extends UntypedConsumerActor {

	private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	@Override
	public String getEndpointUri() {
		// return "restlet:http://localhost:8080/";
		return "restlet:http://localhost:8080/user/{userId}?restletMethods=GET";
	}

	@Override
	public void onReceive(Object message) throws Exception {
		System.out.println(message);
		log.debug("RESTful API got: " + message);

		if (message instanceof CamelMessage) {
			CamelMessage camelMessage = (CamelMessage) message;
			getSender().tell("Word", getSelf());
		} else
			unhandled(message);
	}
}
