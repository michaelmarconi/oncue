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
package oncue.worker;

import oncue.common.messages.Job;

/**
 * A simple sample worker, for demonstration purposes
 */
public class SampleWorker extends AbstractWorker {

	@Override
	protected void doWork(Job job) {
		double progress = 0.0;
		System.out.print("Sample worker doing work on Job " + job.getId() + ".");
		for (int i = 0; i < 3; i++) {
			progress += 0.25;
			System.out.print(".");
			try {
				Thread.sleep(500);
				reportProgress(progress);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Complete!");
		workComplete();
	}

}
