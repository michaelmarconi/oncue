package controllers.api;

import static akka.pattern.Patterns.ask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import akka.actor.ActorRef;
import akka.dispatch.Recover;
import akka.pattern.AskTimeoutException;
import akka.util.Timeout;
import oncue.OnCueService;
import oncue.common.messages.DeleteJob;
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.JobSummary;
import oncue.common.messages.RerunJob;
import oncue.common.messages.SimpleMessages.SimpleMessage;
import oncue.common.serializers.ObjectMapperFactory;
import oncue.common.settings.Settings;
import oncue.common.settings.SettingsProvider;
import play.Logger;
import play.libs.F;
import play.libs.F.Function;
import play.mvc.Controller;
import play.mvc.Result;

public class Jobs extends Controller {

	private final static Settings settings = SettingsProvider.SettingsProvider.get(OnCueService.system());
	private final static ObjectMapper mapper = ObjectMapperFactory.getInstance();

	/**
	 * List all jobs
	 * 
	 * @return a {@linkplain JobSummary}
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
					return ok((JsonNode) mapper.valueToTree(jobSummary.getJobs()));
				}
			}
		});
	}

	/**
	 * Show an individual job
	 * 
	 * @return a {@linkplain JobSummary}
	 */
	public static F.Promise<Result> show(final Long id) {
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
					Job jobToShow = null;
					for (Job job : jobSummary.getJobs()) {
						if (job.getId() == id) {
							jobToShow = job.clonePublicView();
							break;
						}
					}
					if (jobToShow == null)
						throw new RuntimeException("Failed to find a job with ID " + id);

					return ok((JsonNode) mapper.valueToTree(jobToShow));
				}
			}
		});
	}

	/**
	 * Create a new job
	 * 
	 * @return a {@linkplain Job}
	 */
	public static F.Promise<Result> create() {
		EnqueueJob enqueueJob;
		try {
			enqueueJob = mapper.treeToValue(request().body().asJson(), EnqueueJob.class);
		} catch (Exception e) {
			Logger.error("Failed to parse enqueue job request", e);
			return F.Promise.<Result>pure(badRequest(request().body().asJson()));
		}

		ActorRef scheduler = OnCueService.system().actorFor(settings.SCHEDULER_PATH);
		return F.Promise.wrap(
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
					return ok((JsonNode) mapper.valueToTree(response));
				}
			}
		});
	}
	
	/**
	 * Delete an existing job
	 * 
	 * @return a {@linkplain Job}
	 */
	public static F.Promise<Result> delete(final Long id) {
		DeleteJob deleteJob = new DeleteJob(id);
		ActorRef scheduler = OnCueService.system().actorFor(settings.SCHEDULER_PATH);
		return F.Promise.wrap(
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
					return ok((JsonNode) mapper.valueToTree(response));
				}
			}
		});
	}	

	/**
	 * Re-run a job
	 * 
	 * @return a {@linkplain Job}
	 */
	public static F.Promise<Result> rerun(final Long id) {
		RerunJob rerunJob = new RerunJob(id);
		ActorRef scheduler = OnCueService.system().actorFor(settings.SCHEDULER_PATH);
		return F.Promise.wrap(
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
					return ok((JsonNode) mapper.valueToTree(response));
				}
			}
		});
	}
}
