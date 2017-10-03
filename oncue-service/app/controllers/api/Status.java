package controllers.api;

import static akka.pattern.Patterns.ask;

import com.fasterxml.jackson.databind.node.ObjectNode;

import oncue.OnCueService;
import oncue.common.messages.Job;
import oncue.common.messages.JobSummary;
import oncue.common.messages.SimpleMessages.SimpleMessage;
import oncue.common.settings.Settings;
import oncue.common.settings.SettingsProvider;

import play.Logger;
import play.libs.Akka;
import play.libs.F;
import play.libs.F.Function;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import akka.actor.ActorRef;
import akka.dispatch.Recover;
import akka.pattern.AskTimeoutException;
import akka.util.Timeout;

public class Status extends Controller {

	private final static Settings settings = SettingsProvider.SettingsProvider.get(OnCueService.system());

	/**
	 * Returns a JSON object with all relevant service status information
	 */
	public static F.Promise<Result> index() {
		ActorRef scheduler = OnCueService.system().actorFor(settings.SCHEDULER_PATH);
		return F.Promise.wrap(
				ask(scheduler, SimpleMessage.JOB_SUMMARY, new Timeout(settings.SCHEDULER_TIMEOUT)).recover(
						new Recover<Object>() {
							@Override
							public Object recover(Throwable t) {
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
					JobSummary jobSummary = (JobSummary) response;
					int failedJobs = 0;
					int completeJobs = 0;
					for (Job job : jobSummary.getJobs()) {
						switch (job.getState()) {
						case FAILED:
							failedJobs++;
							break;
						case COMPLETE:
							completeJobs++;
							break;
						default:
							break;
						}
					}
					ObjectNode result = Json.newObject();
					result.put("complete_jobs_count", completeJobs);
					result.put("failed_jobs_count", failedJobs);
					return ok(result);
				}
			}
		});
	}
}
