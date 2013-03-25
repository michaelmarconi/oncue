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
package oncue.agent.workers;

import oncue.common.messages.Job;
import oncue.common.messages.JobProgress;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public abstract class AbstractWorker extends UntypedActor {

	protected LoggingAdapter log = Logging.getLogger(getContext().system(),
			this);
	protected ActorRef agent;
	protected Job job;

	/**
	 * Begin working on a job immediately.
	 * 
	 * @param job
	 *            is the specification for the work to be done
	 */
	protected abstract void doWork(Job job);

	@Override
	public void onReceive(Object message) throws Exception {
		if (agent == null)
			agent = getSender();

		if (message instanceof Job) {
			this.job = (Job) message;
			getSender().tell(new JobProgress((Job) message, 0), getSelf());
			doWork((Job) message);
		}
	}

	/**
	 * Report on the percentage progress made on this job.
	 * 
	 * @param progress
	 *            is a double, between 0 and 1
	 */
	public void reportProgress(double progress) {
		if (progress < 0 || progress > 1)
			throw new RuntimeException(
					"Job progress mut be reported as a double between 0 and 1");

		agent.tell(new JobProgress(job, progress), getSelf());
	}

	/**
	 * Indicate that work on this job is complete.
	 */
	public void workComplete() {
		agent.tell(new JobProgress(job, 1), getSelf());
		getContext().stop(getSelf());
	}
}
