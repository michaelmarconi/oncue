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

import static akka.pattern.Patterns.ask;

import java.util.Map;

import oncue.common.exceptions.EnqueueJobException;
import oncue.common.messages.EnqueueJob;
import oncue.common.messages.Job;
import oncue.common.messages.Job.State;
import oncue.common.messages.JobProgress;
import oncue.common.settings.Settings;
import oncue.common.settings.SettingsProvider;
import scala.concurrent.Await;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.AskTimeoutException;
import akka.util.Timeout;

public abstract class AbstractWorker extends UntypedActor {

	protected enum Work {
		COMPLETE, IN_PROGRESS
	}

	protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	protected ActorRef agent;

	protected Job job;

	private boolean isComplete;

	protected Settings settings = SettingsProvider.SettingsProvider.get(getContext().system());

	/**
	 * Begin working on a job immediately.
	 * 
	 * @param job
	 *            is the specification for the work to be done
	 */
	protected abstract Work doWork(Job job) throws Exception;

	@Override
	public void onReceive(Object message) throws Exception {
		if (agent == null)
			agent = getSender();

		if (message instanceof Job) {
			this.job = (Job) message;
			job.setState(State.IN_PROGRESS);
			getSender().tell(new JobProgress(job, 0), getSelf());
			Work state = doWork((Job) message);
			if (!isComplete && state == Work.COMPLETE) {
				workComplete();
			}
		}
	}

	/**
	 * Report on the percentage progress made on this job.
	 * 
	 * @param progress
	 *            is a double, between 0 and 1
	 */
	protected void reportProgress(double progress) {
		if (progress < 0 || progress > 1)
			throw new RuntimeException("Job progress must be reported as a double between 0 and 1");

		agent.tell(new JobProgress(job, progress), getSelf());
	}

	/**
	 * Indicate that work on this job is complete.
	 */
	protected void workComplete() {
		job.setState(State.COMPLETE);
		agent.tell(new JobProgress(job, 1), getSelf());
		isComplete = true;
		getContext().stop(getSelf());
	}

	/**
	 * Submit the job to the specified queue manager.
	 * 
	 * @param workerType
	 *            The qualified class name of the worker to instantiate
	 * @param jobParameters
	 *            The user-defined parameters map to pass to the job
	 * @throws EnqueueJobException
	 *             If the queue manager does not exist or the job is not
	 *             accepted within the timeout
	 */
	protected void enqueueJob(String workerType, Map<String, String> jobParameters) throws EnqueueJobException {

		try {
			Await.result(
					ask(getContext().actorFor(settings.QUEUE_MANAGER_PATH), new EnqueueJob(workerType, jobParameters),
							new Timeout(settings.QUEUE_MANAGER_TIMEOUT)), settings.QUEUE_MANAGER_TIMEOUT);
		} catch (Exception e) {
			if (e instanceof AskTimeoutException) {
				log.error(e, "Timeout waiting for queue manager to enqueue job");
			} else {
				log.error(e, "Failed to enqueue job");
			}

			throw new EnqueueJobException(e);
		}
	}
}
