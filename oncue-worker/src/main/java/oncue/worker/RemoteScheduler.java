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

package oncue.worker;

import java.util.Map;

import oncue.common.exceptions.EnqueueJobException;
import oncue.common.exceptions.RetrieveJobSummaryException;
import oncue.common.messages.JobSummary;

/**
 * This interface is a representation of the remote scheduler component,
 * exposing remote functionality to workers.
 */
public interface RemoteScheduler {

	/**
	 * Submit the job to the scheduler.
	 * 
	 * @param workerType
	 *            The qualified class name of the worker to instantiate
	 * @param jobParameters
	 *            The user-defined parameters map to pass to the job
	 * @throws EnqueueJobException
	 *             If the scheduler does not exist or the job is not accepted
	 *             within the timeout
	 */
	void enqueueJob(String workerType, Map<String, String> jobParameters) throws EnqueueJobException;

	/**
	 * Get a job summary which holds the state of all known jobs in the system,
	 * including the complete and failed jobs.
	 * 
	 * @return a summary of all jobs in the scheduler
	 * @throws RetrieveJobSummaryException
	 */
	JobSummary getJobSummary() throws RetrieveJobSummaryException;

}
