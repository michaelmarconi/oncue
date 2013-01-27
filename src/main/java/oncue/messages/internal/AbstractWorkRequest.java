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
package oncue.messages.internal;

import java.io.Serializable;
import java.util.Collection;

import oncue.agent.internal.AbstractAgent;
import akka.actor.ActorRef;

/**
 * Each concrete implementation of {@linkplain AbstractAgent} will send its own
 * kind of work request to the scheduler, to allow it to include extra
 * information, such as node capacity.
 */
public abstract class AbstractWorkRequest implements Serializable {

	private static final long serialVersionUID = -3802453222882202468L;

	private final ActorRef agent;
	private final Collection<String> workerTypes;

	public AbstractWorkRequest(ActorRef agent, Collection<String> workerTypes) {
		this.agent = agent;
		this.workerTypes = workerTypes;
	}

	public ActorRef getAgent() {
		return agent;
	}

	public Collection<String> getWorkerTypes() {
		return workerTypes;
	}

	@Override
	public String toString() {
		return "Work request from " + agent;
	}

}
