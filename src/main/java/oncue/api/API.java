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
package oncue.api;

import java.util.Map;

import oncue.messages.internal.Job;

/**
 * The interface that defines the available API functionality
 */
public interface API {

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
