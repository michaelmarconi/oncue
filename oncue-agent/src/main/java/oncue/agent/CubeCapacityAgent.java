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

import java.util.Set;

import oncue.common.messages.CubeCapacityWorkRequest;
import oncue.common.messages.Job;

/**
 * TODO
 */
public class CubeCapacityAgent extends AbstractAgent {

	// The amount of total memory
	private final int TOTAL_MEMORY = 5000;

	public CubeCapacityAgent(Set<String> workerTypes) throws MissingWorkerException {
		super(workerTypes);
		log.info("This Cube capacity agent has total memory of {} to work with.", TOTAL_MEMORY);
	}

	@Override
	protected void requestWork() {
		int usedMemory = 0;
		for (Job job : jobsInProgress.values()) {
			usedMemory += new Integer(job.getParams().get("memory"));
		}
		getScheduler().tell(new CubeCapacityWorkRequest(getSelf(), getWorkerTypes(), TOTAL_MEMORY - usedMemory),
				getSelf());
	}
}
