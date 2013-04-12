package controllers.api;

import static akka.pattern.Patterns.ask;

import java.text.SimpleDateFormat;

import oncue.OnCueService;
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.JobSummary;
import oncue.common.messages.SimpleMessages.SimpleMessage;
import oncue.common.settings.Settings;
import oncue.common.settings.SettingsProvider;

import org.codehaus.jackson.map.ObjectMapper;

import play.Logger;
import play.libs.Akka;
import play.libs.F.Function;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import akka.actor.ActorRef;
import akka.dispatch.Recover;
import akka.pattern.AskTimeoutException;
import akka.util.Timeout;

public class Jobs extends Controller {

	private final static Settings settings = SettingsProvider.SettingsProvider.get(OnCueService.system());
	private final static ObjectMapper mapper = new ObjectMapper();

	static {
		mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz"));
	}

	/**
	 * List all jobs
	 * 
	 * @return a {@linkplain JobSummary}
	 */
	public static Result index() {
		ActorRef scheduler = OnCueService.system().actorFor(settings.SCHEDULER_PATH);
		return async(Akka.asPromise(
				ask(scheduler, SimpleMessage.JOB_SUMMARY, new Timeout(settings.SCHEDULER_TIMEOUT)).recover(
						new Recover<Object>() {
							@Override
							public Object recover(Throwable t) throws Throwable {
								if (t instanceof AskTimeoutException) {
									Logger.error("Timeout waiting for scheduler to respond to job summary request", t);
									return internalServerError("Timeout");
								} else {
									Logger.error("Failed to request jobs from scheduler", t);
									return internalServerError("Failed to request jobs from scheduler");
								}
							}
						}, OnCueService.system().dispatcher())).map(new Function<Object, Result>() {
			@Override
			public Result apply(Object response) {
				if (response instanceof Result) {
					// Result objects are returned by the recover handler above
					return (Result) response;
				} else {
					return ok(Json.toJson(response));
				}
			}
		}));
	}

	/**
	 * Create a new job
	 * 
	 * @return a {@linkplain Job}
	 */
	public static Result create() {
		EnqueueJob enqueueJob;
		try {
			enqueueJob = mapper.readValue(request().body().asJson(), EnqueueJob.class);
		} catch (Exception e) {
			Logger.error("Failed to parse enqueue job request", e);
			return badRequest(request().body().asJson());
		}

		ActorRef queueManager = OnCueService.system().actorFor(settings.QUEUE_MANAGER_PATH);
		return async(Akka.asPromise(
				ask(queueManager, enqueueJob, new Timeout(settings.SCHEDULER_TIMEOUT)).recover(new Recover<Object>() {
					@Override
					public Object recover(Throwable t) throws Throwable {
						if (t instanceof AskTimeoutException) {
							Logger.error("Timeout waiting for queue manager to enqueue job", t);
							return internalServerError("Timeout");
						} else {
							Logger.error("Failed to enqueue job", t);
							return internalServerError("Failed to enqueue job");
						}
					}
				}, OnCueService.system().dispatcher())).map(new Function<Object, Result>() {
			@Override
			public Result apply(Object response) {
				if (response instanceof Result) {
					// Result objects are returned by the recover handler above
					return (Result) response;
				} else {
					return ok(mapper.valueToTree(response));
				}
			}
		}));
	}
}
