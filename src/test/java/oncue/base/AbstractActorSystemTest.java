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
package oncue.base;

import oncue.settings.Settings;
import oncue.settings.SettingsProvider;

import org.junit.After;
import org.junit.Before;

import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public abstract class AbstractActorSystemTest {

	protected Config config;
	protected ActorSystem system;
	protected Settings settings;
	protected LoggingAdapter log;

	@Before
	public void flushRedis() {
		startActorSystem();
	}

	private void startActorSystem() {
		config = ConfigFactory.load("test");
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

}
