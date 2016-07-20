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
package oncue.agent;

import java.util.Set;

import oncue.common.messages.CapacityWorkRequest;
import oncue.common.messages.Job;

/**
 * This class is intended to be used with the <code>oncue.scheduler.CapacityScheduler
 * </code>}. It models an agent as a hole of a certain size, indicated by the "memory" priorty. Each
 * job that is sent to the agent will have a "memory" value, and the agent will continuously bite
 * off work to do until it has no more "memory" capacity left, and will execute all jobs in
 * parallel. It will always consume as many jobs as possible and it will do it in the order
 * specified by the <code>oncue.scheduler.PriorityJobComparator</code>. This means that a larger job
 * that cannot be completed at the time may be skipped in favour of a lower priority lower "memory"
 * job. Users must be careful not to provide a continuous stream of low "memory" jobs that could
 * prevent the timely execution of a high "memory" job.
 * 
 * The total "memory" available to the agent must be configured with the configuration propery
 * "oncue.agent.capacity-agent.total-memory". The Agent will crash on startup if this is not
 * provided.
 */
public class CapacityAgent extends AbstractAgent {

	// The amount of total memory. Will fail if not defined.
	private final int TOTAL_MEMORY = getContext().system().settings().config()
			.getInt("oncue.agent.capacity-agent.total-memory");

	public CapacityAgent(Set<String> workerTypes) {
		super(workerTypes);
		log.info("This capacity agent has total memory of {} to work with.", TOTAL_MEMORY);
	}

	@Override
	protected void requestWork() {
		int usedMemory = 0;
		for (Job job : jobsInProgress.values()) {
			usedMemory += Integer.parseInt(job.getParams().get("memory"));
		}
		int availableMemory = TOTAL_MEMORY - usedMemory;
		log.debug("Requesting work with memory capacity of {}", availableMemory);
		getScheduler().tell(
				new CapacityWorkRequest(getSelf(), getWorkerTypes(), availableMemory),
				getSelf());
	}

}