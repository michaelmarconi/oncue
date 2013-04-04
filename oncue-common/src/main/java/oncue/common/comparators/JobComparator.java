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
package oncue.common.comparators;

import java.util.Comparator;

import oncue.common.messages.Job;

/**
 * Compare {@linkplain Job}s by comparing their enqueue times.
 */
public class JobComparator implements Comparator<Job> {

	@Override
	public int compare(Job job1, Job job2) {
		return job1.getEnqueuedAt().compareTo(job2.getEnqueuedAt());
	}

}
