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

import java.util.Collection;

import akka.actor.ActorRef;

public class JVMCapacityWorkRequest extends AbstractWorkRequest {

	private long freeMemory;
	private long totalMemory;
	private long maxMemory;

	private static final long serialVersionUID = -4503173564405936817L;

	public JVMCapacityWorkRequest(ActorRef agent, Collection<String> workerTypes, long freeMemory, long totalMemory,
			long maxMemory) {
		super(agent, workerTypes);
		this.setFreeMemory(freeMemory);
		this.totalMemory = totalMemory;
		this.maxMemory = maxMemory;
	}

	public long getFreeMemory() {
		return freeMemory;
	}

	public long getMaxMemory() {
		return maxMemory;
	}

	public long getTotalMemory() {
		return totalMemory;
	}

	public void setFreeMemory(long freeMemory) {
		this.freeMemory = freeMemory;
	}

	@Override
	public String toString() {
		return super.toString()
				+ String.format(" [freeMem = %s, totalMem = %s, maxMem = %s]", getFreeMemory(), totalMemory, maxMemory);
	}
}
