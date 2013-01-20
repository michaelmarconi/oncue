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
package oncue.agent;

import java.util.List;

import oncue.agent.internal.AbstractAgent;
import oncue.agent.internal.WorkerTypeException;
import oncue.messages.ThrottledWorkRequest;

/**
 * This agent will work on the configured maximum number of jobs at any one
 * time, before asking for more work.
 */
public class ThrottledAgent extends AbstractAgent {

	// The maximum number of concurrent workers
	private final Integer MAX_JOBS;

	public ThrottledAgent(List<String> workerTypes) throws WorkerTypeException {
		super(workerTypes);
		if (settings.THROTTLED_AGENT_JOB_LIMIT == null)
			throw new RuntimeException(
					"Configuration is missing the maximum concurrent jobs configuration for the throttled agent.");

		MAX_JOBS = settings.THROTTLED_AGENT_JOB_LIMIT;
		log.info("The throttled agent will process a maximum of {} jobs in parallel", MAX_JOBS);
	}

	@Override
	protected void requestWork() {

		/*
		 * Don't request work if this agent is already dealing with all the jobs
		 * it can manage
		 */
		if (jobsInProgress.size() < MAX_JOBS) {
			int jobsToRequest = MAX_JOBS - jobsInProgress.size();
			log.debug("Requesting {} new jobs", jobsToRequest);
			getScheduler().tell(new ThrottledWorkRequest(getSelf(), jobsToRequest), getSelf());
		}
	}
}
