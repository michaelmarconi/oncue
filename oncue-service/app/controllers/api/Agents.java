package controllers.api;

import static akka.pattern.Patterns.ask;

import java.text.SimpleDateFormat;

import oncue.OnCueService;
import oncue.common.messages.AgentSummary;
import oncue.common.messages.SimpleMessages.SimpleMessage;
import oncue.common.settings.Settings;
import oncue.common.settings.SettingsProvider;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.PropertyNamingStrategy;
import org.codehaus.jackson.map.SerializationConfig;

import play.Logger;
import play.libs.Akka;
import play.libs.F.Function;
import play.mvc.Controller;
import play.mvc.Result;
import akka.actor.ActorRef;
import akka.dispatch.Recover;
import akka.pattern.AskTimeoutException;
import akka.util.Timeout;

public class Agents extends Controller {

	private final static Settings settings = SettingsProvider.SettingsProvider.get(Akka.system());
	private final static ObjectMapper mapper = new ObjectMapper();

	static {
		mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz"));
		mapper.configure(SerializationConfig.Feature.WRITE_ENUMS_USING_TO_STRING, true);
		mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
	}

	public static Result index() {
		ActorRef scheduler = OnCueService.system().actorFor(settings.SCHEDULER_PATH);
		return async(Akka.asPromise(
				ask(scheduler, SimpleMessage.LIST_AGENTS, new Timeout(settings.SCHEDULER_TIMEOUT)).recover(
						new Recover<Object>() {
							@Override
							public Object recover(Throwable t) throws Throwable {
								if (t instanceof AskTimeoutException) {
									Logger.error("Timeout waiting for scheduler to respond with the list of agents", t);
									return internalServerError("Timeout");
								} else {
									Logger.error("Failed to request registered agents from scheduler", t);
									return internalServerError("Failed to request registered agents from scheduler");
								}
							}
						}, OnCueService.system().dispatcher())).map(new Function<Object, Result>() {
			@Override
			public Result apply(Object response) {
				if (response instanceof Result) {
					// Result objects are returned by the recover handler above
					return (Result) response;
				} else {
					AgentSummary agentSummary = (AgentSummary) response;
					return ok(mapper.valueToTree(agentSummary.getAgents()));
				}
			}
		}));
	}
}
