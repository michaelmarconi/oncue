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

import oncue.agent.internal.AbstractAgent;
import oncue.messages.SimpleWorkRequest;

/**
 * This agent assumes that its host has unlimited capacity and it will respond
 * to every WORK_AVAILABLE message by requesting work.
 * 
 * <b>Use with care</b>, as this agent may run out of resources if you have
 * enough jobs!
 */
public class UnlimitedCapacityAgent extends AbstractAgent {

	@Override
	protected void requestWork() {
		getScheduler().tell(new SimpleWorkRequest(getSelf()), getSelf());
	}

}
