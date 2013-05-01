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
package oncue.common.messages;

import java.util.Set;

import akka.actor.ActorRef;

public class ThrottledWorkRequest extends AbstractWorkRequest {

	private static final long serialVersionUID = -6509063237201496945L;

	private int maxJobs;

	/**
	 * @param jobs
	 *            is the number of jobs the agent can cope with
	 */
	public ThrottledWorkRequest(ActorRef agent, Set<String> workerTypes, int jobs) {
		super(agent, workerTypes);
		this.maxJobs = jobs;
	}

	public int getMaxJobs() {
		return maxJobs;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (String workerType : getWorkerTypes()) {
			builder.append("[" + workerType + "]");
		}
		return "Throttled work request for " + maxJobs + " jobs for worker types: " + builder.toString();
	}

}
