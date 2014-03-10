package oncue.client;

import java.util.Collection;
import java.util.Map;

import oncue.common.messages.Job;

/**
 * A client for submitting jobs to an OnCue service.
 */
public interface Client {

	/**
	 * Enqueue a new job with no parameters
	 * 
	 * @param workerType
	 *            is the type of worker required to complete this job
	 * @return the {@linkplain Job} that was created
	 * @throws ClientException
	 */
	public Job enqueueJob(String workerType) throws ClientException;

	/**
	 * Enqueue a new job with parameters
	 * 
	 * @param workerType
	 *            is the type of worker required to complete this job
	 * @param jobParams
	 *            is a map of string-based job parameters
	 * @return the {@linkplain Job} that was created
	 * @throws ClientException
	 */
	public Job enqueueJob(String workerType, Map<String, String> jobParams) throws ClientException;

	/**
	 * Retrieve a list of all known jobs in the system,
	 * including the complete and failed jobs.
	 * 
	 * @return a {@link Collection} of all {@linkplain Job}s
	 * @throws ClientException
	 */
	public Collection<Job> getJobs() throws ClientException;

}