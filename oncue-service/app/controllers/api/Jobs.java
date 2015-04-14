package controllers.api;

import static akka.pattern.Patterns.ask;

import java.util.List;
import java.util.ArrayList;

import java.text.SimpleDateFormat;

import oncue.OnCueService;
import oncue.common.messages.DeleteJob;
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.JobSummary;
import oncue.common.messages.RerunJob;
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

public class Jobs extends Controller {

	private final static Settings settings = SettingsProvider.SettingsProvider.get(OnCueService.system());
	private final static ObjectMapper mapper = new ObjectMapper();

	static {
		mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz"));
		mapper.configure(SerializationConfig.Feature.WRITE_ENUMS_USING_TO_STRING, true);
		mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
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
					return ok(mapper.valueToTree(jobSummary.getJobs()));
				}
			}
		}));
	}

	/**
	 * Show an individual job
	 * 
	 * @return a {@linkplain JobSummary}
	 */
	public static Result show(final Long id) {
		ActorRef scheduler = OnCueService.system().actorFor(settings.SCHEDULER_PATH);
		return async(Akka.asPromise(
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
					Job jobToShow = null;
					for (Job job : jobSummary.getJobs()) {
						if (job.getId() == id) {
							jobToShow = job.clonePublicView();
							break;
						}
					}
					if (jobToShow == null)
						throw new RuntimeException("Failed to find a job with ID " + id);

					return ok(mapper.valueToTree(jobToShow));
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

		ActorRef scheduler = OnCueService.system().actorFor(settings.SCHEDULER_PATH);
		return async(Akka.asPromise(
				ask(scheduler, enqueueJob, new Timeout(settings.SCHEDULER_TIMEOUT)).recover(new Recover<Object>() {
					@Override
					public Object recover(Throwable t) {
						if (t instanceof AskTimeoutException) {
							Logger.error("Timeout waiting for scheduler to enqueue job", t);
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
	
	/**
	 * Delete an existing job
	 * 
	 * @return a {@linkplain Job}
	 */
	public static Result delete(final Long id) {
		DeleteJob deleteJob = new DeleteJob(id);
		ActorRef scheduler = OnCueService.system().actorFor(settings.SCHEDULER_PATH);
		return async(Akka.asPromise(
				ask(scheduler, deleteJob, new Timeout(settings.SCHEDULER_TIMEOUT)).recover(new Recover<Object>() {
					@Override
					public Object recover(Throwable t) {
						if (t instanceof AskTimeoutException) {
							Logger.error("Timeout waiting for scheduler to delete job", t);
							return internalServerError("Timeout");
						} else {
							Logger.error("Failed to delete job", t);
							return internalServerError("Failed to delete job");
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

	/**
	 * Re-run a job
	 * 
	 * @return a {@linkplain Job}
	 */
	public static Result rerun(final Long id) {
		RerunJob rerunJob = new RerunJob(id);
		ActorRef scheduler = OnCueService.system().actorFor(settings.SCHEDULER_PATH);
		return async(Akka.asPromise(
				ask(scheduler, rerunJob, new Timeout(settings.SCHEDULER_TIMEOUT)).recover(new Recover<Object>() {
					@Override
					public Object recover(Throwable t) {
						if (t instanceof AskTimeoutException) {
							Logger.error("Timeout waiting for queue manager to enqueue a job to re-run", t);
							return internalServerError("Timeout");
						} else {
							Logger.error("Failed to enqueue a job to re-run", t);
							return internalServerError("Failed to enqueue job to re-run");
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
