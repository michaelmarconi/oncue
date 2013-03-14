package oncue.messages.internal;

import java.io.Serializable;
import java.util.Map;

public class RetryTimedJobMessage implements Serializable {

	private static final long serialVersionUID = 2209290741530342714L;

	private String workerType;

	private Map<String, String> jobParameters;

	public RetryTimedJobMessage(String workerType, Map<String, String> jobParameters) {
		this.workerType = workerType;
		this.jobParameters = jobParameters;
	}

	
	public String getWorkerType() {
		return workerType;
	}

	
	public void setWorkerType(String workerType) {
		this.workerType = workerType;
	}

	
	public Map<String, String> getJobParameters() {
		return jobParameters;
	}

	
	public void setJobParameters(Map<String, String> jobParameters) {
		this.jobParameters = jobParameters;
	}

}
