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
package oncue;

import oncue.settings.Settings;
import oncue.settings.SettingsProvider;
import akka.actor.Actor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class OnCue {

	@Parameter(names = "-environment", description = "The environment configuration to load")
	private String environment = "production";

	@Parameter(names = "-components", required = true, validateWith = ComponentParameterValidator.class, description = "The components to run (one of 'service', 'agent' or 'api')")
	private String componentType;

	@Parameter(names = { "-help", "-h" }, hidden = true, help = true)
	private boolean help;

	// The app configuration
	private static Config config;

	@SuppressWarnings("all")
	private static void createServiceComponents(ActorSystem system, final Settings settings) {

		// Start the queue manager
		system.actorOf(new Props(new UntypedActorFactory() {
			@Override
			public Actor create() throws Exception {
				return (Actor) Class.forName(settings.QUEUE_MANAGER_CLASS).newInstance();
			}
		}), settings.QUEUE_MANAGER_NAME);

		// Start the scheduler
		system.actorOf(new Props(new UntypedActorFactory() {
			@Override
			public Actor create() throws Exception {
				Class schedulerClass = Class.forName(settings.SCHEDULER_CLASS);
				Class backingStoreClass = null;
				if (settings.SCHEDULER_BACKING_STORE_CLASS != null)
					backingStoreClass = Class.forName(settings.SCHEDULER_BACKING_STORE_CLASS);
				return (Actor) schedulerClass.getConstructor(Class.class).newInstance(backingStoreClass);
			}
		}), settings.SCHEDULER_NAME);
	}

	@SuppressWarnings("serial")
	public static void main(String[] args) {
		OnCue onCue = new OnCue();
		JCommander commander = new JCommander(onCue, args);

		if (onCue.help) {
			commander.usage();
			return;
		}

		// Load the environment configuration
		config = ConfigFactory.load(onCue.environment);

		// Load the components
		switch (onCue.componentType) {
		case "service":
			ActorSystem system = ActorSystem.create("oncue-service", config.getConfig("service").withFallback(config));
			final Settings serviceSettings = SettingsProvider.SettingsProvider.get(system);
			createServiceComponents(system, serviceSettings);
			break;

		case "agent":
			system = ActorSystem.create("oncue-agent", config.getConfig("client").withFallback(config));
			final Settings agentSettings = SettingsProvider.SettingsProvider.get(system);
			system.actorOf(new Props(new UntypedActorFactory() {

				@Override
				public Actor create() throws Exception {
					return (Actor) Class.forName(agentSettings.AGENT_CLASS).newInstance();
				}
			}), agentSettings.AGENT_NAME);
			break;

		}
	}
}
