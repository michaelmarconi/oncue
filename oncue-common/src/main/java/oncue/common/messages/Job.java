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
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

public class Job extends UnmodifiableJob implements Serializable, Cloneable {

	private static final long serialVersionUID = 1855878065709808580L;

	/**
	 * This default constructor required for Jackson JSON serialization
	 */
	public Job() {
		this.setEnqueuedAt(new DateTime(DateTimeUtils.currentTimeMillis()));
		this.state = State.QUEUED;
		this.progress = 0.0;
		this.setRerun(false);
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

		if (this.getParams() != null) {
			Map<String, String> paramsClone = new HashMap<>();
			for (String key : this.getParams().keySet()) {
				paramsClone.put(key, this.params.get(key));
			}
			clone.setParams(paramsClone);
		}

		return clone;
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

}
