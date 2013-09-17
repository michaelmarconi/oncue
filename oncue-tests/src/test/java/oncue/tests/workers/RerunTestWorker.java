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
package oncue.tests.workers;

import oncue.common.messages.Job;
import oncue.worker.AbstractWorker;
import redis.clients.jedis.Jedis;

/**
 * A test worker that leaves external evidence in Redis that it has been
 * re-run.
 */
public class RerunTestWorker extends AbstractWorker {

	@Override
	public void doWork(Job job) throws InterruptedException {
		Thread.sleep(500);
		log.info("Test job ran normally");
	}

	@Override
	protected void redoWork(Job job) throws Exception {
		Thread.sleep(500);
		log.info("Test job was re-run!");
		Jedis jedis = new Jedis("localhost");
		jedis.set("oncue.tests.workers.RerunTestworker", "re-run");
	}

}
