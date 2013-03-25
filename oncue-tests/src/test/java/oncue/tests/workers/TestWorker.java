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

import oncue.agent.workers.AbstractWorker;
import oncue.common.messages.Job;

public class TestWorker extends AbstractWorker {

	@Override
	public void doWork(Job job) {
		double progress = 0.0;
		for (int i = 0; i < 3; i++) {
			progress += 0.25;
			try {
				Thread.sleep(500);
				reportProgress(progress);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		workComplete();
	}

}
