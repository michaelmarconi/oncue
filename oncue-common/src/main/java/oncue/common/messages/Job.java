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
package oncue.common.messages;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;

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
		},
		DELETED {
			public String toString() {
				return "deleted";
			}
		}
	}

	private static final long serialVersionUID = -2375588116753600617L;

	private DateTime enqueuedAt = new DateTime(DateTimeUtils.currentTimeMillis());
	private Map<String, String> params = Maps.newHashMap();
	private double progress = 0.0;
	private boolean rerun = false;
	private State state = State.QUEUED;

	private DateTime startedAt;
	private DateTime completedAt;
	private String errorMessage;
	private long id;
	private String workerType;

	/**
	 * This default constructor required for Jackson JSON serialization
	 */
	public Job() {
	}

	/**
	 * Create a new job. Use this constructor when you are creating a job for the first time. The
	 * job enqueued time will be set to now and the job state will be set to queued.
	 * 
	 * @param id is the unique identifier for this job
	 * 
	 * @param workerType determines which type of worker is capable of completing this job
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
		clone.setStartedAt(this.getStartedAt());
		clone.setCompletedAt(this.getCompletedAt());
		clone.setErrorMessage(this.getErrorMessage());
		clone.setProgress(this.getProgress());
		clone.setState(this.getState());
		clone.setRerun(this.isRerun());

		for (String key : this.getParams().keySet()) {
			clone.getParams().put(key, this.params.get(key));
		}

		return clone;
	}

	// Create a clone of this job with publicly visible properties.
	public Job clonePublicView() {
		Job result = (Job) clone();
		for (Iterator<String> it = result.getParams().keySet().iterator(); it.hasNext(); ) {
			String key = it.next();
			if (key.charAt(0) == '$') {
				result.getParams().put(key, "*hidden*");
			}
		}
		return result;
	}

	public DateTime getCompletedAt() {
		return completedAt;
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

	public double getProgress() {
		return progress;
	}

	public DateTime getStartedAt() {
		return startedAt;
	}

	public State getState() {
		return state;
	}

	public String getWorkerType() {
		return workerType;
	}

	public boolean isRerun() {
		return rerun;
	}

	public void setCompletedAt(DateTime completedAt) {
		this.completedAt = completedAt;
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

	public void setRerun(boolean rerun) {
		this.rerun = rerun;
	}

	public void setStartedAt(DateTime startedAt) {
		this.startedAt = startedAt;
	}

	public void setState(State state) {
		this.state = state;
	}

	// Don't print parameters that start with $
	public Map<String,String> getPrintableParams() {
		Map<String,String> result = new HashMap<>();
		for (String key : params.keySet()) {
			if (key.charAt(0) != '$') {
				result.put(key, params.get(key));
			} else {
				result.put(key, "*hidden*");
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return String
				.format("Job %s (state=%s, enqueuedAt=%s, startedAt=%s, completedAt=%s, workerType=%s, re-run=%s, progress=%s params=%s)",
						id, state, getEnqueuedAt(), getStartedAt(), getCompletedAt(), workerType,
						rerun, progress, getPrintableParams());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Job otherJob = (Job) obj;
		return Objects.equal(enqueuedAt, otherJob.getEnqueuedAt())
				&& Objects.equal(startedAt, otherJob.getStartedAt())
				&& Objects.equal(completedAt, otherJob.getCompletedAt())
				&& Objects.equal(errorMessage, otherJob.getErrorMessage())
				&& Objects.equal(id, otherJob.getId())
				&& Objects.equal(params, otherJob.getParams())
				&& Objects.equal(progress, otherJob.getProgress())
				&& Objects.equal(rerun, otherJob.isRerun())
				&& Objects.equal(state, otherJob.getState())
				&& Objects.equal(workerType, otherJob.getWorkerType());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(enqueuedAt, startedAt, completedAt, errorMessage, id, params,
				progress, rerun, state, workerType);
	}
}
