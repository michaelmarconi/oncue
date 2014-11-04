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
package oncue.common.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.Extension;

import com.typesafe.config.Config;

public class Settings implements Extension {

	private static final String SCHEDULER_BACKING_STORE_PATH = "scheduler.backing-store.class";
	public final String SCHEDULER_NAME;
	public final String SCHEDULER_PATH;
	public final String SCHEDULER_CLASS;
	public String SCHEDULER_BACKING_STORE_CLASS;
	public final FiniteDuration SCHEDULER_TIMEOUT;
	public final FiniteDuration SCHEDULER_BROADCAST_JOBS_FREQUENCY;
	public final FiniteDuration SCHEDULER_BROADCAST_JOBS_QUIESCENCE_PERIOD;
	public final FiniteDuration SCHEDULER_MONITOR_AGENTS_FREQUENCY;
	public final FiniteDuration SCHEDULER_AGENT_HEARTBEAT_TIMEOUT;

	public final FiniteDuration TIMED_JOBS_RETRY_DELAY;

	public final String AGENT_NAME;
	public final String AGENT_PATH;
	public final String AGENT_CLASS;
	public final FiniteDuration AGENT_HEARTBEAT_FREQUENCY;

	public final List<Map<String, Object>> TIMED_JOBS_TIMETABLE;

	@SuppressWarnings("unchecked")
	public Settings(Config config) {

		Config oncueConfig = config.getConfig("oncue");

		SCHEDULER_NAME = oncueConfig.getString("scheduler.name");
		SCHEDULER_PATH = oncueConfig.getString("scheduler.path");
		SCHEDULER_CLASS = oncueConfig.getString("scheduler.class");
		SCHEDULER_TIMEOUT = Duration
				.create(oncueConfig.getMilliseconds("scheduler.response-timeout"), TimeUnit.MILLISECONDS);

		if(oncueConfig.hasPath(SCHEDULER_BACKING_STORE_PATH)) {
			SCHEDULER_BACKING_STORE_CLASS = oncueConfig.getString(SCHEDULER_BACKING_STORE_PATH);
		} else {
			SCHEDULER_BACKING_STORE_CLASS = null;
		}

		SCHEDULER_BROADCAST_JOBS_FREQUENCY = Duration.create(
				oncueConfig.getMilliseconds("scheduler.broadcast-jobs-frequency"), TimeUnit.MILLISECONDS);

		SCHEDULER_BROADCAST_JOBS_QUIESCENCE_PERIOD = Duration.create(
				oncueConfig.getMilliseconds("scheduler.broadcast-jobs-quiescence-period"), TimeUnit.MILLISECONDS);

		SCHEDULER_MONITOR_AGENTS_FREQUENCY = Duration.create(
				oncueConfig.getMilliseconds("scheduler.monitor-agents-frequency"), TimeUnit.MILLISECONDS);

		SCHEDULER_AGENT_HEARTBEAT_TIMEOUT = Duration.create(
				oncueConfig.getMilliseconds("scheduler.agent-heartbeat-timeout"), TimeUnit.MILLISECONDS);

		AGENT_NAME = oncueConfig.getString("agent.name");
		AGENT_PATH = oncueConfig.getString("agent.path");
		AGENT_CLASS = oncueConfig.getString("agent.class");
		AGENT_HEARTBEAT_FREQUENCY = Duration.create(oncueConfig.getMilliseconds("agent.heartbeat-frequency"),
				TimeUnit.MILLISECONDS);

		TIMED_JOBS_RETRY_DELAY = Duration.create(oncueConfig.getMilliseconds("timed-jobs.retry-delay"),
				TimeUnit.MILLISECONDS);

		// Timed jobs are optional
		if (oncueConfig.hasPath("timed-jobs.timetable")) {
			TIMED_JOBS_TIMETABLE = (ArrayList<Map<String, Object>>) oncueConfig.getAnyRef("timed-jobs.timetable");
		} else {
			TIMED_JOBS_TIMETABLE = null;
		}
	}
}
