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

public class CubeCapacityWorkRequest extends AbstractWorkRequest {

	private static final long serialVersionUID = 8422903458993615943L;

	private int availableMemory;

	/**
	 * @param availableMemory
	 *            is the amount of free memory the agent has.
	 */
	public CubeCapacityWorkRequest(ActorRef agent, Set<String> workerTypes, int availableMemory) {
		super(agent, workerTypes);
		this.availableMemory = availableMemory;
	}

	public int getAvailableMemory() {
		return availableMemory;
	}

	@Override
	public String toString() {
		return "Cube capacity work request for a maximum of " + availableMemory + " with of jobs";
	}

}
