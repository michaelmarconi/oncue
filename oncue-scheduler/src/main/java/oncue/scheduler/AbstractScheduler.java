/*******************************************************************************
 * Copyright 2013 Michael Marconi
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package oncue.scheduler;

import static java.lang.String.format;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import oncue.backingstore.BackingStore;
import oncue.common.events.AgentStartedEvent;
import oncue.common.events.AgentStoppedEvent;
import oncue.common.events.JobCleanupEvent;
import oncue.common.events.JobEnqueuedEvent;
import oncue.common.events.JobFailedEvent;
import oncue.common.events.JobProgressEvent;
import oncue.common.exceptions.DeleteJobException;
import oncue.common.messages.AbstractWorkRequest;
import oncue.common.messages.Agent;
import oncue.common.messages.AgentSummary;
import oncue.common.messages.CleanupJobs;
import oncue.common.messages.DeleteJob;
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.Job.State;
import oncue.common.messages.JobFailed;
import oncue.common.messages.JobProgress;
import oncue.common.messages.JobSummary;
import oncue.common.messages.RerunJob;
import oncue.common.messages.SimpleMessages.SimpleMessage;
import oncue.common.messages.WorkAvailable;
import oncue.common.messages.WorkResponse;
import oncue.common.settings.Settings;
import oncue.common.settings.SettingsProvider;
import oncue.scheduler.exceptions.JobNotFoundException;
import oncue.scheduler.exceptions.ScheduleException;
import scala.concurrent.duration.Deadline;
import akka.actor.ActorInitializationException;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Status.Failure;
import akka.actor.Status.Success;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.remote.RemoteClientShutdown;

/**
 * A scheduler is responsible for keeping a list of registered agents,
 * broadcasting new work to them when it arrives and distributing the work using
 * a variety of scheduling algorithms, depending on the concrete implementation.
 */
public abstract class AbstractScheduler<WorkRequest extends AbstractWorkRequest> extends UntypedActor {

	// A periodic check for dead agents
	private Cancellable agentMonitor;

	// Map an agent to a deadline for deregistration
	private Map<String, Deadline> agents = new HashMap<>();

	// Map an agent to a the set of worker types it can process
	private Map<String, Set<String>> agentWorkers = new HashMap<>();

	// The persistent backing store
	protected BackingStore backingStore;

	// A scheduled check for jobs to broadcast
	private Cancellable jobsBroadcast;

	protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	// A flag to indicate that jobs should not be scheduled temporarily
	private boolean paused = false;

	// The map of scheduled jobs
	private ScheduledJobs scheduledJobs;

	protected Settings settings = SettingsProvider.SettingsProvider.get(getContext().system());

	// A probe for testing
	private ActorRef testProbe;

	// The queue of unscheduled jobs
	protected UnscheduledJobs unscheduledJobs;

	/**
	 * @param backingStore
	 *            is an implementation of {@linkplain BackingStore}
	 */
	public AbstractScheduler(Class<? extends BackingStore> backingStore) {

		if (backingStore == null)
			throw new RuntimeException("A backing store implementation must be specified!");

		try {
			this.backingStore = backingStore.getConstructor(ActorSystem.class, Settings.class).newInstance(
					getContext().system(), settings);
			unscheduledJobs = new UnscheduledJobs(this.backingStore, log);
			scheduledJobs = new ScheduledJobs(this.backingStore);
			log.info("{} is running, backed by {}", getClass().getSimpleName(), backingStore.getSimpleName());
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new ActorInitializationException(getSelf(), "Failed to create a backing store from class: "
					+ backingStore.getName(), e);
		}
	}

	/**
	 * While there are jobs in the queue, continue sending a "Work available"
	 * message to all registered agents.
	 */
	private void broadcastJobs() {

		/*
		 * Don't broadcast jobs if there are no agents, no more jobs on the
		 * unscheduled queue or scheduling is paused
		 */
		if (agents.size() == 0 || unscheduledJobs.isEmpty() || paused)
			return;

		log.debug("Broadcasting jobs");

		for (String agent : agents.keySet()) {
			if (testProbe != null)
				testProbe.tell(createWorkAvailable(), getSelf());
			getContext().actorFor(agent).tell(createWorkAvailable(), getSelf());
		}

		// Tee-up another broadcast if necessary
		if (unscheduledJobs.getSize() > 0) {

			// Cancel any scheduled broadcast
			if (jobsBroadcast != null)
				jobsBroadcast.cancel();

			jobsBroadcast = getContext().system().scheduler()
					.scheduleOnce(settings.SCHEDULER_BROADCAST_JOBS_FREQUENCY, new Runnable() {

						@Override
						public void run() {
							getSelf().tell(SimpleMessage.BROADCAST_JOBS, getSelf());
						}
					}, getContext().dispatcher());
		}
	}

	/**
	 * Check to see that each agent has sent a heart beat by the deadline.
	 */
	private void checkAgents() {
		for (String agent : agents.keySet()) {
			Deadline deadline = agents.get(agent);

			if (deadline.isOverdue()) {
				log.error("Found a dead agent: '{}'", agent);

				if (testProbe != null)
					testProbe.tell(SimpleMessage.AGENT_DEAD, getSelf());

				deregisterAgent(agent);
				rebroadcastJobs(agent);
			}
		}
	}

	/**
	 * When a job is finished or has failed, it must be removed from the
	 * scheduler's records.
	 * 
	 * @param job
	 *            is the {@linkplain Job} to clean up after
	 */
	private void cleanupJob(Job job, String agent) {
		log.debug("Cleaning up {} for agent {}", job, agent);
		scheduledJobs.removeJob(job, agent);
	}

	/**
	 * Construct a message to advertise the type of work available.
	 */
	private WorkAvailable createWorkAvailable() {
		return new WorkAvailable(unscheduledJobs.getWorkerTypes());
	}

	/**
	 * Delete an existing job
	 * 
	 * @param job
	 *            is the job to delete
	 * @return the deleted job
	 * @throws DeleteJobException
	 *             if the job is currently running
	 */
	private Job deleteJob(Job job) throws DeleteJobException {
		switch (job.getState()) {
		case RUNNING:
			throw new DeleteJobException("This job cannot be deleted as it is currently running");
		case QUEUED:
			boolean removed = unscheduledJobs.removeJob(job);
			if (!removed)
				throw new DeleteJobException("Failed to remove the job from the unscheduled jobs queue");
			break;
		case COMPLETE:
			backingStore.removeCompletedJob(job);
			break;
		case FAILED:
			backingStore.removeFailedJob(job);
			break;
		default:
			throw new DeleteJobException(job.getState().toString() + " is an unrecognised job state");
		}
		job.setState(State.DELETED);
		return job;
	}

	/**
	 * De-register an agent
	 */
	private void deregisterAgent(String url) {
		agents.remove(url);
		agentWorkers.remove(url);

		// Stop listening to remote events
		getContext().system().eventStream().unsubscribe(getContext().actorFor(url));

		// Broadcast agent stopped event
		Agent agent = new Agent(url);
		getContext().system().eventStream().publish(new AgentStoppedEvent(agent));

	}

	/**
	 * Dispatch jobs to agents according to entries in the schedule. This method
	 * will also keep record of the jobs scheduled to each agent, in case an
	 * agent dies.
	 * 
	 * @param schedule
	 *            is the {@linkplain Schedule} that maps agents to jobs
	 */
	protected void dispatchJobs(Schedule schedule) {
		validateSchedule(schedule);
		for (Map.Entry<String, WorkResponse> entry : schedule.getEntries()) {
			ActorRef agent = getContext().actorFor(entry.getKey());
			WorkResponse workResponse = entry.getValue();

			// Assign the jobs to the agent
			unscheduledJobs.removeJobs(workResponse.getJobs());
			scheduledJobs.addJobs(agent.path().toString(), workResponse.getJobs());

			// Tell the agent about the work
			agent.tell(workResponse, getSelf());
		}
	}

	/**
	 * Enqueue a new job
	 */
	private Job enqueueJob(EnqueueJob enqueueJob) {
		Job job = new Job(backingStore.getNextJobID(), enqueueJob.getWorkerType());
		if (enqueueJob.getParams() != null)
			job.setParams(enqueueJob.getParams());
		unscheduledJobs.addJob(job);
		getContext().system().eventStream().publish(new JobEnqueuedEvent(job));
		startJobsBroadcast();
		return job;
	}

	/**
	 * Look through all jobs to find an existing job
	 * 
	 * @param id
	 *            is the unique job identifier
	 * @return the matching job
	 * @throws JobNotFoundException
	 */
	private Job findExistingJob(long id) throws JobNotFoundException {
		Set<Job> jobs = getAllJobs();
		for (Job job : jobs) {
			if (job.getId() == id)
				return job;
		}
		throw new JobNotFoundException("Failed to find an existing job with ID " + id);
	}

	/**
	 * @return the set of all registered agents
	 */
	protected Set<String> getAgents() {
		return agents.keySet();
	}

	/**
	 * @return the map of agents to the worker types they can process
	 */
	protected Map<String, Set<String>> getAgentWorkers() {
		return agentWorkers;
	}

	/**
	 * @return the full set of unscheduled, scheduled, complete and failed jobs
	 */
	private Set<Job> getAllJobs() {
		Set<Job> jobs = new HashSet<>();
		for (Iterator<Job> iterator = unscheduledJobs.iterator(); iterator.hasNext();) {
			jobs.add(iterator.next());
		}
		jobs.addAll(scheduledJobs.getJobs());
		jobs.addAll(backingStore.getCompletedJobs());
		jobs.addAll(backingStore.getFailedJobs());

		return jobs;
	}

	/**
	 * Record the details of a failed job
	 * 
	 * @param jobFailed
	 *            contains both the failed job and the cause of failure
	 */
	private void handleJobFailure(Job job, String agent) {
		if (backingStore != null)
			backingStore.persistJobFailure(job);

		cleanupJob(job, agent);

		getContext().system().eventStream().publish(new JobFailedEvent(job));
	}

	/**
	 * Record any progress made against a job. If the job is complete, remove it
	 * from the jobs scheduled against an agent.
	 * 
	 * @param jobProgress
	 *            describes the job and associated progress.
	 */
	private void handleJobProgress(Job job, String agent) {
		if (backingStore != null)
			backingStore.persistJobProgress(job);

		if (job.getProgress() == 1.0) {
			log.debug("{} is complete.", job);
			cleanupJob(job, agent);
		} else if (job.getState() != State.QUEUED)
			scheduledJobs.updateJob(job, agent);

		getContext().system().eventStream().publish(new JobProgressEvent(job));
	}

	/**
	 * Inject a probe into this actor for testing
	 * 
	 * @param testProbe
	 *            is a JavaTestKit probe
	 */
	public void injectProbe(ActorRef testProbe) {
		this.testProbe = testProbe;
	}

	/**
	 * Set up a monitor that periodically checks for dead Agents
	 */
	private void monitorAgents() {
		agentMonitor = getContext()
				.system()
				.scheduler()
				.schedule(settings.SCHEDULER_MONITOR_AGENTS_FREQUENCY, settings.SCHEDULER_MONITOR_AGENTS_FREQUENCY,
						new Runnable() {

							@Override
							public void run() {
								getSelf().tell(SimpleMessage.CHECK_AGENTS, getSelf());
							}
						}, getContext().dispatcher());
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onReceive(Object message) throws Exception {

		if (testProbe != null)
			testProbe.forward(message, getContext());

		if (message.equals(SimpleMessage.AGENT_HEARTBEAT)) {
			log.debug("Got a heartbeat from agent: '{}'", getSender());
			registerAgent(getSender().path().toString());
		}

		else if (message instanceof RemoteClientShutdown) {
			String system = ((RemoteClientShutdown) message).getRemoteAddress().system();
			if (system.equals("oncue-agent")) {
				String agent = ((RemoteClientShutdown) message).getRemoteAddress().toString() + settings.AGENT_PATH;
				log.info("Agent '{}' has shut down", agent);
				deregisterAgent(agent);
				rebroadcastJobs(agent);

				if (testProbe != null)
					testProbe.tell(SimpleMessage.AGENT_SHUTDOWN, getSelf());
			}
		}

		else if (message.equals(SimpleMessage.CHECK_AGENTS)) {
			log.debug("Checking for dead agents...");
			checkAgents();
		}

		else if (message instanceof EnqueueJob) {
			log.debug("Got a new job to enqueue: {}", message);
			Job job = enqueueJob((EnqueueJob) message);
			getSender().tell(job, getSelf());
		}

		else if (message instanceof RerunJob) {
			log.debug("Got an existing job to re-run: {}", message);
			Job job = findExistingJob(((RerunJob) message).getId());
			Job rerunJob = rerunJob(job);
			getSender().tell(rerunJob, getSelf());
		}

		else if (message instanceof DeleteJob) {
			log.debug("Got an existing job to delete: {}", message);
			Job job = findExistingJob(((DeleteJob) message).getId());
			Job deleteJob;
			try {
				deleteJob = deleteJob(job);
				getSender().tell(deleteJob, getSelf());
			} catch (DeleteJobException e) {
				log.error(e, "Failed to delete job {}", job.getId());
				getSender().tell(new Failure(e), getSelf());
			}
		}

		else if (message instanceof CleanupJobs) {
			log.debug("Clean up jobs");
			CleanupJobs cleanupJobs = (CleanupJobs) message;
			int numCleanedJobs = backingStore.cleanupJobs(cleanupJobs.isIncludeFailedJobs(),
					cleanupJobs.getExpirationAge());
			getContext().system().eventStream().publish(new JobCleanupEvent());
			getSender().tell(new Success(format("Removed %d jobs", numCleanedJobs)), getSelf());
		}

		else if (message instanceof AbstractWorkRequest) {
			log.debug("Got a work request from agent '{}': {}", getSender().path().toString(), message);
			AbstractWorkRequest workRequest = (AbstractWorkRequest) message;
			agentWorkers.put(getSender().path().toString(), workRequest.getWorkerTypes());
			boolean workAvailable = unscheduledJobs.isWorkAvailable(workRequest.getWorkerTypes());
			if (!workAvailable || paused)
				replyWithNoWork(getSender());
			else {
				scheduleJobs((WorkRequest) workRequest);
			}
		}

		else if (message instanceof JobProgress) {
			Job job = ((JobProgress) message).getJob();
			log.debug("Agent reported progress of {} on {}", job.getProgress(), job);
			handleJobProgress(job, getSender().path().toString());
		}

		else if (message instanceof JobFailed) {
			Job job = ((JobFailed) message).getJob();
			log.debug("Agent reported a failed job {} ({})", job, job.getErrorMessage());
			handleJobFailure(job, getSender().path().toString());
		}

		else if (message == SimpleMessage.JOB_SUMMARY) {
			log.debug("Received a request for a job summary from {}", getSender());
			replyWithJobSummary();
		}

		else if (message == SimpleMessage.LIST_AGENTS) {
			log.debug("Received a request for a the list of registered agents from {}", getSender());
			replyWithAgentSummary();
		}

		else if (message.equals(SimpleMessage.BROADCAST_JOBS)) {
			log.debug("Teeing up a job broadcast...");
			broadcastJobs();
		}

		else {
			log.error("Unrecognised message: {}", message);
			unhandled(message);
		}
	}

	/**
	 * Pause job scheduling temporarily
	 */
	public void pause() {
		paused = true;
	}

	@Override
	public void postStop() {
		super.postStop();

		if (agentMonitor != null)
			agentMonitor.cancel();
		if (jobsBroadcast != null)
			jobsBroadcast.cancel();

		log.info("Shut down.");
	}

	@Override
	public void preStart() {
		monitorAgents();
		super.preStart();
	}

	/**
	 * In the case where an Agent has died or shutdown before completing the
	 * jobs assigned to it, we need to re-broadcast the jobs so they are run by
	 * another agent.
	 * 
	 * @param agent
	 *            is the Agent to check for incomplete jobs
	 */
	private void rebroadcastJobs(String agent) {
		if (!scheduledJobs.getJobs(agent).isEmpty()) {

			// Grab the list of jobs scheduled for this agent
			List<Job> agentJobs = new ArrayList<>();
			for (Job scheduledJob : scheduledJobs.getJobs(agent)) {
				agentJobs.add(scheduledJob);
			}

			for (Job job : agentJobs) {

				// Remove job from the agent
				scheduledJobs.removeJob(job, agent);

				// Reset job state and progress
				job.setState(State.QUEUED);
				job.setProgress(0);
				handleJobProgress(job, agent);

				// Add jobs back onto the unscheduled queue
				unscheduledJobs.addJob(job);
			}
		}
		broadcastJobs();
	}

	/**
	 * Register the heartbeat of an agent, capturing the heartbeat time as a
	 * timestamp. If this is a new Agent, return a message indicating that it
	 * has been registered.
	 * 
	 * @param agent
	 *            is the agent to register
	 */
	private void registerAgent(String url) {
		if (!agents.containsKey(url)) {
			Agent agent = new Agent(url);
			getContext().actorFor(url).tell(SimpleMessage.AGENT_REGISTERED, getSelf());
			getContext().system().eventStream().subscribe(getSelf(), RemoteClientShutdown.class);
			getContext().system().eventStream().publish(new AgentStartedEvent(agent));
			log.info("Registered agent: {}", url);
		}

		agents.put(url, settings.SCHEDULER_AGENT_HEARTBEAT_TIMEOUT.fromNow());
	}

	/**
	 * Reply with the list of registered agents
	 */
	private void replyWithAgentSummary() {
		List<Agent> agents = new ArrayList<>();
		for (String url : this.agents.keySet()) {
			Agent agent = new oncue.common.messages.Agent(url);
			agents.add(agent);
		}
		getSender().tell(new AgentSummary(agents), getSelf());
	}

	/**
	 * Construct and reply with a job summary message
	 */
	private void replyWithJobSummary() {
		List<Job> jobs = new ArrayList<>();
		jobs.addAll(getAllJobs());
		JobSummary response = new JobSummary(jobs);
		getSender().tell(response, getSelf());
	}

	/**
	 * Send a response to the requesting agent containing a
	 * {@linkplain WorkResponse} with no jobs.
	 */
	private void replyWithNoWork(ActorRef agent) {
		agent.tell(new WorkResponse(), getSelf());
	}

	/**
	 * Re-run an existing job
	 * 
	 * @param job
	 *            is the job to re-run
	 */
	private Job rerunJob(Job job) {
		Job rerunJob = new Job(job.getId(), job.getWorkerType());
		if (job.getParams() != null)
			rerunJob.setParams(job.getParams());
		rerunJob.setRerun(true);

		// TODO Find a way to make this transactional
		unscheduledJobs.addJob(rerunJob);
		if (job.getState() == Job.State.COMPLETE)
			backingStore.removeCompletedJob(job);
		else if (job.getState() == Job.State.FAILED)
			backingStore.removeFailedJob(job);

		getContext().system().eventStream().publish(new JobEnqueuedEvent(job));
		startJobsBroadcast();
		return rerunJob;
	}

	/**
	 * Create a schedule that maps agents to work responses. Once the schedule
	 * has been created, the work should be dispatched by calling the
	 * <i>dispatchJobs</i> method.
	 */
	protected abstract void scheduleJobs(WorkRequest workRequest);

	/**
	 * Schedule a jobs broadcast. Cancel any previously scheduled broadcast, to
	 * ensure quiescence in the case where lots of new jobs arrive in a short
	 * time.
	 */
	private void startJobsBroadcast() {
		if (jobsBroadcast != null && !jobsBroadcast.isCancelled())
			jobsBroadcast.cancel();

		jobsBroadcast = getContext().system().scheduler()
				.scheduleOnce(settings.SCHEDULER_BROADCAST_JOBS_QUIESCENCE_PERIOD, new Runnable() {

					@Override
					public void run() {
						getSelf().tell(SimpleMessage.BROADCAST_JOBS, getSelf());
					}
				}, getContext().dispatcher());
	}

	/**
	 * Allow the scheduler to continue scheduling jobs.
	 */
	public void unpause() {
		paused = false;
	}

	/**
	 * Ensure that the schedule produced by the scheduler is valid, e.g. ensure
	 * that no agent is scheduled a job it does not have the worker to process.
	 * 
	 * @param schedule
	 *            is the {@linkplain Schedule} to validate
	 */
	private void validateSchedule(Schedule schedule) {
		for (Map.Entry<String, WorkResponse> entry : schedule.getEntries()) {
			String agent = entry.getKey();
			WorkResponse workResponse = entry.getValue();
			for (Job job : workResponse.getJobs()) {
				boolean foundWorkerType = false;
				for (String workerType : agentWorkers.get(agent)) {
					if (job.getWorkerType().equals(workerType)) {
						foundWorkerType = true;
						break;
					}
				}
				if (!foundWorkerType)
					throw new ScheduleException("Agent " + agent + " was assigned " + job.toString()
							+ ", but does not have a worker capable of processing it!");
			}
		}
	}
}
