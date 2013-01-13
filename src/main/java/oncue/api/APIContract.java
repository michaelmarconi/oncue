package oncue.api;

import java.util.Map;

import oncue.messages.internal.Job;

public interface APIContract {

	/**
	 * Enqueue a new job with no parameters
	 * 
	 * @param workerType
	 *            is the type of worker required to complete this job
	 * @return the {@linkplain Job} that was created
	 * @throws APIException
	 */
	public Job enqueueJob(String workerType) throws APIException;

	/**
	 * Enqueue a new job with parameters
	 * 
	 * @param workerType
	 *            is the type of worker required to complete this job
	 * @param jobParams
	 *            is a map of string-based job parameters
	 * @return the {@linkplain Job} that was created
	 * @throws APIException
	 */
	public Job enqueueJob(String workerType, Map<String, String> jobParams) throws APIException;
	
}
