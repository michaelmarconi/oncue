package oncue.common.messages;

import java.util.Map;

import org.joda.time.DateTime;

public abstract class UnmodifiableJob {
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

	protected DateTime enqueuedAt;
	protected DateTime startedAt;
	protected DateTime completedAt;
	protected String errorMessage;
	protected long id;
	protected Map<String, String> params;
	protected Double progress;
	protected boolean rerun;
	protected State state;
	protected String workerType;



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

	public Double getProgress() {
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

	@Override
	public String toString() {
		return String
				.format("Job %s (state=%s, enqueuedAt=%s, startedAt=%s, completedAt=%s, workerType=%s, re-run=%s, progress=%s)",
						id, state, getEnqueuedAt(), getStartedAt(), getCompletedAt(), workerType, rerun, progress);
	}
}
