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
import com.typesafe.config.ConfigException;

public class Settings implements Extension {

	public final String SCHEDULER_NAME;
	public final String SCHEDULER_PATH;
	public final String SCHEDULER_CLASS;
	public String SCHEDULER_BACKING_STORE_CLASS;
	public final FiniteDuration SCHEDULER_BROADCAST_JOBS_FREQUENCY;
	public final FiniteDuration SCHEDULER_BROADCAST_JOBS_QUIESCENCE_PERIOD;
	public final FiniteDuration SCHEDULER_MONITOR_AGENTS_FREQUENCY;
	public final FiniteDuration SCHEDULER_AGENT_HEARTBEAT_TIMEOUT;

	public final String QUEUE_MANAGER_NAME;
	public final String QUEUE_MANAGER_CLASS;
	public final String QUEUE_MANAGER_PATH;
	public final FiniteDuration QUEUE_MANAGER_TIMEOUT;
	
	public final FiniteDuration TIMED_JOB_RETRY_DELAY;

	public final String AGENT_NAME;
	public final String AGENT_PATH;
	public final String AGENT_CLASS;
	public final FiniteDuration AGENT_HEARTBEAT_FREQUENCY;

	public final String API_NAME;
	public final FiniteDuration API_TIMEOUT;

	public Integer THROTTLED_AGENT_JOB_LIMIT;
	public final List<Map<String, Object>> TIMETABLE;

	@SuppressWarnings("unchecked")
	public Settings(Config config) {
		config = config.getConfig("oncue");
		
		SCHEDULER_NAME = config.getString("scheduler.name");
		SCHEDULER_PATH = config.getString("scheduler.path");
		SCHEDULER_CLASS = config.getString("scheduler.class");

		try {
			SCHEDULER_BACKING_STORE_CLASS = config
					.getString("scheduler.backing-store-class");
		} catch (ConfigException.Missing e) {
			SCHEDULER_BACKING_STORE_CLASS = null;
		}

		SCHEDULER_BROADCAST_JOBS_FREQUENCY = Duration.create(
				config.getMilliseconds("scheduler.broadcast-jobs-frequency"),
				TimeUnit.MILLISECONDS);

		SCHEDULER_BROADCAST_JOBS_QUIESCENCE_PERIOD = Duration.create(config
				.getMilliseconds("scheduler.broadcast-jobs-quiescence-period"),
				TimeUnit.MILLISECONDS);

		SCHEDULER_MONITOR_AGENTS_FREQUENCY = Duration.create(
				config.getMilliseconds("scheduler.monitor-agents-frequency"),
				TimeUnit.MILLISECONDS);

		SCHEDULER_AGENT_HEARTBEAT_TIMEOUT = Duration.create(
				config.getMilliseconds("scheduler.agent-heartbeat-timeout"),
				TimeUnit.MILLISECONDS);

		QUEUE_MANAGER_NAME = config.getString("queue-manager.name");
		QUEUE_MANAGER_CLASS = config.getString("queue-manager.class");
		QUEUE_MANAGER_PATH = config.getString("queue-manager.path");
		QUEUE_MANAGER_TIMEOUT = Duration.create(config.getMilliseconds("queue-manager.timeout"),
				TimeUnit.MILLISECONDS);
		
		TIMED_JOB_RETRY_DELAY = Duration.create(config.getMilliseconds("timed-jobs.retry-delay"),
				TimeUnit.MILLISECONDS);		

		API_NAME = config.getString("api.name");
		API_TIMEOUT = Duration.create(config.getMilliseconds("api.timeout"),
				TimeUnit.MILLISECONDS);

		AGENT_NAME = config.getString("agent.name");
		AGENT_PATH = config.getString("agent.path");
		AGENT_CLASS = config.getString("agent.class");
		AGENT_HEARTBEAT_FREQUENCY = Duration.create(
				config.getMilliseconds("agent.heartbeat-frequency"),
				TimeUnit.MILLISECONDS);

		THROTTLED_AGENT_JOB_LIMIT = config.getInt("agent.throttled-agent.max-jobs");

		// Timed job schedules are optional
		if (config.hasPath("timetable")) {
			TIMETABLE = (ArrayList<Map<String, Object>>) config
					.getAnyRef("timetable");
		} else {
			TIMETABLE = null;
		}
	}
}
