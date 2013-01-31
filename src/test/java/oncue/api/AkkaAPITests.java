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
package oncue.api;

import static junit.framework.Assert.assertEquals;
import oncue.messages.internal.Job;
import oncue.queueManager.InMemoryQueueManager;
import oncue.settings.Settings;
import oncue.settings.SettingsProvider;
import oncue.workers.TestWorker;

import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.testkit.JavaTestKit;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class AkkaAPITests {

	protected static Config config;
	protected ActorSystem system;
	protected Settings settings;
	protected LoggingAdapter log;

	@Before
	public void loadConfig() {
		config = ConfigFactory.load("api-test");
	}

	@Test(expected = APIException.class)
	public void enqueueJobWithNoQueueManagerRunning() throws APIException {
		AkkaAPI.getInstance(config).enqueueJob(TestWorker.class.getName());
	}

	@Test
	public void enqueueJobWithQueueManagerRunning() throws APIException {
		system = ActorSystem.create("oncue-service", config.getConfig("service").withFallback(config));
		settings = SettingsProvider.SettingsProvider.get(system);
		log = Logging.getLogger(system, this);

		new JavaTestKit(system) {
			{
				// Create a queue manager
				system.actorOf(new Props(InMemoryQueueManager.class), settings.QUEUE_MANAGER_NAME);

				// Use the API to enqueue a new job
				Job job = AkkaAPI.getInstance(config).enqueueJob(TestWorker.class.getName());

				assertEquals(1, job.getId());
				assertEquals(TestWorker.class.getName(), job.getWorkerType());
			}
		};

	}

}
