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
package oncue.tests.workers;

import java.util.Collection;

import oncue.common.messages.Job;
import oncue.common.messages.JobSummary;
import oncue.worker.AbstractWorker;
import redis.clients.jedis.Jedis;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class JobSummaryRequestTestWorker extends AbstractWorker {

	@Override
	public void doWork(Job job) throws Exception {
		Collection<Job> jobs = getSchedulerClient().getJobs();
		Jedis jedis = new Jedis("localhost");
		JobSummary jobSummary = new JobSummary(jobs);
		jedis.set("oncue.tests.workers.JobSummaryRequestTestWorker", jobSummary.toString());
	}

	@Override
	protected void redoWork(Job job) throws Exception {
		throw new NotImplementedException();
	}
}
