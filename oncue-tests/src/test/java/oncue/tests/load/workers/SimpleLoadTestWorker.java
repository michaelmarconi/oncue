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
package oncue.tests.load.workers;

import oncue.common.messages.Job;
import oncue.worker.AbstractWorker;

public class SimpleLoadTestWorker extends AbstractWorker {

	private static final int LOAD_FACTOR = 10000;

	@Override
	protected JobState doWork(Job job) {

		int count = 0;
		while (count < LOAD_FACTOR) {
			count++;

			/*
			 * Yield occasionally, so we don't starve the Agent of CPU and
			 * prevent its heart beat.
			 */
			if (count % 10 == 0)
				Thread.yield();
		}

		return JobState.COMPLETE;
	}

}
