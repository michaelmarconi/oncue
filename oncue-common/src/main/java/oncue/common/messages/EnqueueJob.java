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

import java.io.Serializable;
import java.util.Map;

/**
 * This message is sent to the queue manager (a concrete implementation of
 * {@linkplain oncue.queueManager.internal.AbstractQueueManager} in order to
 * create and enqueue a new job.
 */
public class EnqueueJob implements Serializable {

	private static final long serialVersionUID = -1624154938276876491L;

	private String workerType;
	private Map<String, String> jobParams;

	public EnqueueJob(String workerType, Map<String, String> jobParams) {
		this.workerType = workerType;
		this.jobParams = jobParams;
	}

	public EnqueueJob(String workerType) {
		this(workerType, null);
	}

	public Map<String, String> getJobParams() {
		return jobParams;
	}

	public String getWorkerType() {
		return workerType;
	}

}
