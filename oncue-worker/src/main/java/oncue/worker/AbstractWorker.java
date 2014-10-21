/*******************************************************************************
 * Copyright 2013 Michael Marconi
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package oncue.worker;

import oncue.client.AkkaClient;
import oncue.client.Client;
import oncue.common.messages.Job;
import oncue.common.messages.JobProgress;
import oncue.common.messages.UnmodifiableJob.State;
import oncue.common.settings.Settings;
import oncue.common.settings.SettingsProvider;

import org.joda.time.DateTime;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public abstract class AbstractWorker extends UntypedActor {

	protected ActorRef agent;

	protected Job job;

	protected LoggingAdapter log = Logging.getLogger(getContext().system(),
			this);

	protected Settings settings = SettingsProvider.SettingsProvider
			.get(getContext().system());

	private Client client = new AkkaClient(getContext().system(), getContext()
			.actorFor(settings.SCHEDULER_PATH));

	/**
	 * Begin working on a job immediately. Once the worker returns from this
	 * method, we assume the work on the job is complete.
	 * 
	 * @param job
	 *            is the specification for the work to be done
	 */
	protected abstract void doWork(Job job) throws Exception;

	/**
	 * @return an implementation of {@linkplain Client}, which - * represents
	 *         the functionality available on the remote scheduler - *
	 *         component. This is useful if a worker wants to enqueue a job or -
	 *         * ask for the list of jobs at the scheduler. -
	 */
	protected Client getSchedulerClient() {
		return client;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (agent == null)
			agent = getSender();

		if (message instanceof Job) {
			this.job = (Job) message;
			prepareWork();
			if (job.isRerun())
				redoWork(job);
			else
				doWork(job);
			workComplete();
		}
	}

	/**
	 * Update the job state let the agent know we have begun working.
	 */
	private void prepareWork() {
		job.setState(State.RUNNING);
		job.setProgress(0.0);
		job.setStartedAt(DateTime.now());
		getSender().tell(new JobProgress(job), getSelf());
	}

	/**
	 * Re-do work that may or may not have completed successfully previously, so
	 * the worker may need to take compensating action. Once the worker returns
	 * from this method, we assume the work on the job is complete.
	 * 
	 * @param job
	 *            is the specification for the work to be done
	 */
	protected abstract void redoWork(Job job) throws Exception;

	/**
	 * Report on the percentage progress made on this job.
	 * 
	 * @param progress
	 *            is a double, between 0 and 1
	 */
	protected void reportProgress(double progress) {
		if (progress < 0 || progress > 1)
			throw new RuntimeException(
					"Job progress must be reported as a double between 0 and 1");
		job.setProgress(progress);
		agent.tell(new JobProgress(job), getSelf());
	}

	/**
	 * Indicate that work on this job is complete.
	 */
	private void workComplete() {
		job.setState(State.COMPLETE);
		job.setProgress(1);
		job.setCompletedAt(DateTime.now());
		log.debug("Work on {} is complete.", job);
		agent.tell(new JobProgress(job), getSelf());
		getContext().stop(getSelf());
	}
}
