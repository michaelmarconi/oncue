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
import java.util.Map;

import org.joda.time.DateTime;

public class Job implements Serializable {

	public enum State {
		FAILED
	}

	private static final long serialVersionUID = -2375588116753600617L;

	private final DateTime enqueuedAt;
	private final String workerType;
	private long id;
	private Map<String, String> params;
	private Double progress;

	public Job(long id, DateTime enqueuedAt, String workerType) {
		this(id, enqueuedAt, workerType, null);
	}

	/**
	 * Create a new job specification
	 * 
	 * @param id
	 *            is the unique identifier for this job
	 * 
	 * @param enqueuedAt
	 *            is the time at which this job was enqueued
	 * 
	 * @param workerType
	 *            determines which type of {@linkplain IWorker} is capable of
	 *            completing this job
	 * 
	 * @param params
	 *            is an unbounded list of String-based parameters
	 */
	public Job(long id, DateTime enqueuedAt, String workerType, Map<String, String> params) {
		this.id = id;
		this.enqueuedAt = enqueuedAt;
		this.workerType = workerType;
		this.params = params;
	}

	public DateTime getEnqueuedAt() {
		return enqueuedAt;
	}

	public long getId() {
		return id;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public Double getProgress() {
		return progress;
	}

	public String getWorkerType() {
		return workerType;
	}

	public void setProgress(double progress) {
		this.progress = progress;
	}

	@Override
	public String toString() {
		return String.format("Job %s (enqueuedAt=%s, workerType=%s)", id, enqueuedAt, workerType);
	}
}
