package oncue.tests.timedjobs;

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

import oncue.tests.base.AbstractActorSystemTest;
import oncue.timedjobs.TimedJobFactory;

import org.junit.Test;

import akka.actor.EmptyLocalActorRef;
import akka.testkit.JavaTestKit;

public class TimedJobFactoryTest extends AbstractActorSystemTest {

	@Test
	public void createTimedJob() {
		new JavaTestKit(system) {
			{
				TimedJobFactory.createTimedJobs(system, settings.TIMED_JOBS_TIMETABLE);

				new AwaitCond(duration("5 seconds"), duration("1 second")) {
					@Override
					protected boolean cond() {
						boolean timedJobANotFound = system.actorFor("akka://oncue-test/user/job-timer-test-worker-1") instanceof EmptyLocalActorRef;
						boolean timedJobBNotFound = system.actorFor("akka://oncue-test/user/job-timer-test-worker-2") instanceof EmptyLocalActorRef;

						return !timedJobANotFound && !timedJobBNotFound;
					}
				};
			}
		};
	}
}
