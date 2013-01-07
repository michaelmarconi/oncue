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
package oncue.agent.internal;

import static akka.actor.SupervisorStrategy.stop;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oncue.comparators.JobComparator;
import oncue.messages.internal.Job;
import oncue.messages.internal.JobFailed;
import oncue.messages.internal.JobProgress;
import oncue.messages.internal.SimpleMessages.SimpleMessage;
import oncue.messages.internal.WorkResponse;
import oncue.settings.Settings;
import oncue.settings.SettingsProvider;
import oncue.worker.internal.AbstractWorker;

import org.jboss.netty.channel.socket.Worker;

import scala.concurrent.duration.Duration;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.Scheduler;
import akka.actor.SupervisorStrategy;
import akka.actor.SupervisorStrategy.Directive;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;

public abstract class AbstractAgent extends UntypedActor {

	protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	final protected Settings settings = SettingsProvider.SettingsProvider.get(getContext().system());
	private ActorRef testProbe;

	// The scheduled heartbeat
	private Cancellable heartbeat;

	// A scheduled request for new work
	private Cancellable workRequest;

	// Map jobs in progress to their workers
	protected Map<ActorRef, Job> jobsInProgress = new HashMap<ActorRef, Job>();

	/**
	 * @return a reference to the scheduler
	 */
	protected ActorRef getScheduler() {
		return getContext().actorFor(settings.SCHEDULER_PATH);
	}

	public void injectProbe(ActorRef testProbe) {
		this.testProbe = testProbe;
	}

	/**
	 * Note the progress against a job. If it is complete, remove it from the
	 * jobs in progress map.
	 * 
	 * @param progress
	 *            is the {@linkplain JobProgress} made against a job
	 * @param worker
	 *            is the {@linkplain AbstractWorker} completing the job
	 */
	private void recordProgress(JobProgress progress, ActorRef worker) {
		getScheduler().tell(progress, getSelf());
		if (progress.getProgress() == 1.0) {
			jobsInProgress.remove(worker);
			scheduleWorkRequest();
		}
	}

	/**
	 * Schedule a work request to take place to allow for a period of quiesence
	 * after job completion.
	 */
	private void scheduleWorkRequest() {
		if (workRequest != null && !workRequest.isCancelled())
			workRequest.cancel();

		workRequest = getContext().system().scheduler().scheduleOnce(Duration.Zero(), new Runnable() {

			@Override
			public void run() {
				requestWork();
			}
		}, getContext().dispatcher());
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (testProbe != null)
			testProbe.forward(message, getContext());

		if (message.equals(SimpleMessage.REGISTERED)) {
			log.info("Registered with scheduler");
			requestWork();
		}

		else if (message instanceof WorkResponse) {
			log.debug("Got a response to my work request: {}", message);
			List<Job> jobs = ((WorkResponse) message).getJobs();
			Collections.sort(jobs, new JobComparator());
			for (Job job : jobs) {
				spawnWorker(job);
			}
		}

		else if (message.equals(SimpleMessage.WORK_AVAILABLE)) {
			log.debug("Work is available");
			requestWork();
		}

		else if (message instanceof JobProgress) {
			log.debug("Worker reported progress of {} on job {}", ((JobProgress) message).getProgress(),
					((JobProgress) message).getJob());
			recordProgress((JobProgress) message, getSender());
		}

		else {
			log.error("Unrecognised message: {}", message);
			unhandled(message);
		}
	}

	@Override
	public void postStop() {
		super.postStop();
		heartbeat.cancel();
	}

	@Override
	public void preStart() {
		super.preStart();
		log.info("{} is running", getClass().getSimpleName());
		heartbeat = getContext().system().scheduler()
				.schedule(Duration.Zero(), settings.AGENT_HEARTBEAT_FREQUENCY, new Runnable() {

					@Override
					public void run() {
						getScheduler().tell(SimpleMessage.HEARTBEAT, getSelf());
					}
				}, getContext().dispatcher());
	}

	/**
	 * Request work from the {@linkplain Scheduler}.
	 */
	protected abstract void requestWork();

	/**
	 * Spawn a new worker to complete a job.
	 * 
	 * @param job
	 *            is the job that a {@linkplain Worker} should complete.
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("serial")
	private void spawnWorker(Job job) throws InstantiationException, ClassNotFoundException {
		final Class<?> workerClass = Class.forName(job.getWorkerType());

		if (!AbstractWorker.class.isAssignableFrom(workerClass))
			throw new InstantiationException(
					String.format("Cannot create a worker from type %s, as it does not extend an AbstractWorker",
							job.getWorkerType()));

		ActorRef worker = getContext().actorOf(new Props(new UntypedActorFactory() {
			@Override
			public Actor create() throws Exception {
				return (Actor) workerClass.newInstance();
			}
		}), "job-" + job.getId());
		jobsInProgress.put(worker, job);
		worker.tell(job, getSelf());
	}

	/**
	 * Supervise all workers for unexpected exceptions. When an exception is
	 * encountered, tell the scheduler about it, stop the worker and remove it
	 * from the jobs in progress map.
	 */
	@Override
	public SupervisorStrategy supervisorStrategy() {
		return new OneForOneStrategy(0, Duration.Zero(), new Function<Throwable, Directive>() {

			@Override
			public Directive apply(Throwable error) throws Exception {
				log.error(error, "The worker {} has died a horrible death!", getSender());
				Job job = jobsInProgress.remove(getSender());
				getScheduler().tell(new JobFailed(job, error), getSelf());
				return stop();
			}
		});
	}
}
