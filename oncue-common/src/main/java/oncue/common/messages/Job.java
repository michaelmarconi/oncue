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
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

public class Job implements Serializable, Cloneable {

	public enum State {
		COMPLETE {
			public String toString() {
				return "complete";
			}
		},
		FAILED {
			public String toString() {
				return "failed";
			}
		},
		QUEUED {
			public String toString() {
				return "queued";
			}
		},
		RUNNING {
			public String toString() {
				return "running";
			}
		},
		SCHEDULED {
			public String toString() {
				return "scheduled";
			}
		}
	}

	private static final long serialVersionUID = -2375588116753600617L;

	private DateTime enqueuedAt;
	private String errorMessage;
	private long id;
	private Map<String, String> params;
	private Double progress;
	private State state;
	private String workerType;

	/**
	 * This default constructor required for Jackson JSON serialization
	 */
	public Job() {
		this.setEnqueuedAt(new DateTime(DateTimeUtils.currentTimeMillis()));
		this.state = State.QUEUED;
		this.progress = 0.0;
	}

	/**
	 * Create a new job. Use this constructor when you are creating a job for
	 * the first time. The job enqueued time will be set to now and the job
	 * state will be set to queued.
	 * 
	 * @param id
	 *            is the unique identifier for this job
	 * 
	 * @param workerType
	 *            determines which type of worker is capable of completing this
	 *            job
	 */
	public Job(long id, String workerType) {
		this();
		this.id = id;
		this.workerType = workerType;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		Job clone = new Job(this.getId(), this.getWorkerType());
		clone.setEnqueuedAt(this.getEnqueuedAt());
		clone.setErrorMessage(this.getErrorMessage());
		clone.setProgress(this.getProgress());
		clone.setState(this.getState());

		if (this.getParams() != null) {
			Map<String, String> paramsClone = new HashMap<>();
			for (String key : this.getParams().keySet()) {
				paramsClone.put(key, this.params.get(key));
			}
			clone.setParams(paramsClone);
		}
		
		return clone;
	}

	public DateTime getEnqueuedAt() {
		return enqueuedAt;
	}

	public String getErrorMessage() {
		return errorMessage;
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

	public State getState() {
		return state;
	}

	public String getWorkerType() {
		return workerType;
	}

	public void setEnqueuedAt(DateTime enqueuedAt) {
		this.enqueuedAt = enqueuedAt;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public void setProgress(double progress) {
		this.progress = progress;
	}

	public void setState(State state) {
		this.state = state;
	}

	@Override
	public String toString() {
		return String.format("Job %s (state=%s, enqueuedAt=%s, workerType=%s, progress=%s)", id, state,
				getEnqueuedAt(), workerType, progress);
	}

}
