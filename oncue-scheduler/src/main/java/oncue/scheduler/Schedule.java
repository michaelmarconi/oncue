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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import oncue.common.messages.Job;
import oncue.common.messages.WorkResponse;
import sun.management.Agent;
import akka.actor.ActorRef;

/**
 * A Schedule describes the set of {@linkplain Job}s assigned to each
 * {@linkplain Agent}.
 */
public class Schedule {

	private Map<String, WorkResponse> schedule = new HashMap<String, WorkResponse>();

	/**
	 * @return the entry set for this schedule
	 */
	public Set<Entry<String, WorkResponse>> getEntries() {
		return schedule.entrySet();
	}

	/**
	 * Assign a single job to an agent
	 */
	public void setJob(ActorRef agent, Job job) {
		schedule.put(agent.path().toString(), new WorkResponse(job));
	}

	/**
	 * Assign a list of jobs to an agent
	 */
	public void setJobs(ActorRef agent, List<Job> jobs) {
		schedule.put(agent.path().toString(), new WorkResponse(jobs));
	}
}
