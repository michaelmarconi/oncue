package controllers.api;

import static akka.pattern.Patterns.ask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import akka.actor.ActorRef;
import akka.dispatch.Recover;
import akka.pattern.AskTimeoutException;
import akka.util.Timeout;
import oncue.OnCueService;
import oncue.common.messages.AgentSummary;
import oncue.common.messages.SimpleMessages.SimpleMessage;
import oncue.common.serializers.ObjectMapperFactory;
import oncue.common.settings.Settings;
import oncue.common.settings.SettingsProvider;
import play.Logger;
import play.libs.Akka;
import play.libs.F;
import play.libs.F.Function;
import play.mvc.Controller;
import play.mvc.Result;

public class Agents extends Controller {

	private final static Settings settings = SettingsProvider.SettingsProvider.get(Akka.system());
	private final static ObjectMapper mapper = ObjectMapperFactory.getInstance();

	public static F.Promise<Result> index() {
		ActorRef scheduler = OnCueService.system().actorFor(settings.SCHEDULER_PATH);
		return F.Promise.wrap(
				ask(scheduler, SimpleMessage.LIST_AGENTS, new Timeout(settings.SCHEDULER_TIMEOUT)).recover(
						new Recover<Object>() {
							@Override
							public Object recover(Throwable t) {
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
					return ok((JsonNode) mapper.valueToTree(agentSummary.getAgents()));
				}
			}
		});
	}
}
