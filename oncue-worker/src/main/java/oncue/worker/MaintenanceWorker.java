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
package oncue.worker;

import static akka.pattern.Patterns.ask;

import java.util.concurrent.TimeoutException;

import oncue.common.messages.CleanupJobs;
import oncue.common.messages.Job;

import org.joda.time.Duration;

import scala.concurrent.Await;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.Timeout;

/**
 * This job will clean up complete and failed runs from the backing store.
 * You'll probably want to run this as a timed job on a schedule.
 */
public class MaintenanceWorker extends AbstractWorker {

	private static final String EXPIRATION_AGE = "expiration-age";
	private static final String INCLUDE_FAILED_JOBS = "include-failed-jobs";
	private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	@Override
	public void doWork(Job job) throws Exception {
		processJob();
	}

	private void processJob() throws Exception {

		boolean includeFailedJobs = false;
		if (job.getParams().containsKey(INCLUDE_FAILED_JOBS))
			includeFailedJobs = new Boolean(job.getParams().get(INCLUDE_FAILED_JOBS));

		if (!job.getParams().containsKey(EXPIRATION_AGE))
			throw new IllegalArgumentException(
					"You must define the 'expiration-age' parameter in the configuration of a maintenance worker");

		Duration expirationAge = Duration.millis(scala.concurrent.duration.Duration.create(
				job.getParams().get(EXPIRATION_AGE)).toMillis());
		CleanupJobs cleanupJobs = new CleanupJobs(includeFailedJobs, expirationAge);

		try {
			Object object = Await.result(
					ask(getContext().actorFor(settings.SCHEDULER_PATH), cleanupJobs, new Timeout(
							settings.SCHEDULER_TIMEOUT)), settings.SCHEDULER_TIMEOUT);
			log.info(object.toString());
		} catch (TimeoutException e) {
			throw new RuntimeException("Timeout waiting for scheduler to clean up jobs", e);
		}
	}

	@Override
	protected void redoWork(Job job) throws Exception {
		processJob();
	}
}
