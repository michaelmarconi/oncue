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
package oncue.queueManager.internal;

import java.util.Map;

import oncue.messages.internal.EnqueueJob;
import oncue.messages.internal.Job;
import oncue.settings.Settings;
import oncue.settings.SettingsProvider;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Extend this class to implement a new queue manager
 */
public abstract class AbstractQueueManager extends UntypedActor {

	protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	protected Settings settings = SettingsProvider.SettingsProvider.get(getContext().system());

	// A probe for testing
	private ActorRef testProbe;

	public AbstractQueueManager() {
		log.info("{} is running", getClass().getSimpleName());
	}

	/**
	 * Create a new {@linkplain Job}
	 * 
	 * @param workerType
	 *            is the type of worker required to complete this job
	 * 
	 * @param jobParams
	 *            is a map of String-based parameters
	 * 
	 * @return a new {@linkplain Job}
	 */
	protected abstract Job createJob(String workerType, Map<String, String> jobParams);

	public LoggingAdapter getLog() {
		return log;
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

	public Settings getSettings() {
		return settings;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		
		if (testProbe != null)
			testProbe.forward(message, getContext());

		if (message instanceof EnqueueJob) {
			EnqueueJob enqueueJob = (EnqueueJob) message;
			Job job = createJob(enqueueJob.getWorkerType(), enqueueJob.getJobParams());
			getSender().tell(job, getSelf());
		} else
			unhandled(message);
	}

	@Override
	public void postStop() {
		super.postStop();
		log.info("Shut down.");
	}
}
