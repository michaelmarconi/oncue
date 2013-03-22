package oncue.tests.timedJobs;

/*
 * #%L Oncue $Id:$ $HeadURL:$ %% Copyright (C) 2012 - 2013 Michael Marconi %% Licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License. #L%
 */

import oncue.common.settings.Settings;
import oncue.common.settings.SettingsProvider;
import oncue.service.timedjobs.TimedJobFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorSystem;
import akka.actor.EmptyLocalActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.testkit.JavaTestKit;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TimedJobFactoryTest {

	private Config config;

	private ActorSystem system;

	private Settings settings;

	private LoggingAdapter log;

	@Before
	public void startActorSystem() {
		config = ConfigFactory.load("timetable-test");
		system = ActorSystem.create("oncue-test", config);
		settings = SettingsProvider.SettingsProvider.get(system);
		log = Logging.getLogger(system, this);
	}

	@After
	public void stopActorSystem() throws Exception {
		system.shutdown();
		while (!system.isTerminated()) {
			log.debug("Waiting for system to shut down...");
			Thread.sleep(500);
		}
		log.debug("System shut down");
	}

	@Test
	public void createTimedJob() {
		new JavaTestKit(system) {
			{
				TimedJobFactory.createJobsFromJobMap(system, settings.TIMETABLE);

				new AwaitCond(duration("5 seconds"), duration("1 second")) {
					@Override
					protected boolean cond() {
						boolean timedJobANotFound = system
								.actorFor("akka://oncue-test/user/job-timer-test-worker-1") instanceof EmptyLocalActorRef;
						boolean timedJobBNotFound = system
								.actorFor("akka://oncue-test/user/job-timer-test-worker-2") instanceof EmptyLocalActorRef;

						return !timedJobANotFound && !timedJobBNotFound;
					}
				};
			}
		};
	}
}
