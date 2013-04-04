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

public class JobProgress implements Serializable {

	private static final long serialVersionUID = 3715228030356566880L;

	private final Job job;
	private final double progress;

	public JobProgress(Job job, double progress) {
		this.job = job;
		this.progress = progress;
	}

	public Job getJob() {
		return job;
	}

	public double getProgress() {
		return progress;
	}

	@Override
	public String toString() {
		return job + ("( " + progress + ")");
	}

}
