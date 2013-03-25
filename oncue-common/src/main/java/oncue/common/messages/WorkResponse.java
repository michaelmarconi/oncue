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
import java.util.ArrayList;
import java.util.List;

public class WorkResponse implements Serializable {

	private static final long serialVersionUID = 2445495404623836556L;

	private final List<Job> jobs = new ArrayList<Job>();

	public WorkResponse() {
	}

	public WorkResponse(Job job) {
		this.jobs.add(job);
	}

	public WorkResponse(List<Job> jobs) {
		this.jobs.addAll(jobs);
	}

	public List<Job> getJobs() {
		return jobs;
	}

	@Override
	public String toString() {
		if (jobs.isEmpty())
			return "No jobs";
		else
			return jobs.size() + " jobs";
	}
}
