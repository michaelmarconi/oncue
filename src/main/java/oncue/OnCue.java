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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oncue.api.API;
import oncue.api.APIException;
import oncue.messages.internal.Job;
import oncue.settings.Settings;
import oncue.settings.SettingsProvider;
import akka.actor.Actor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Defines the entry points into the OnCue job scheduling framework.
 * 
 * -start scheduler/agent -api
 */
public class OnCue {

	@Parameters(commandDescription = "Enqueue a new job")
	private static class EnqueueJobCommand {

		@Parameter(names = "-worker", required = true, description = "The type of worker required to run this job")
		private static String workerType;

		@Parameter(names = "-param", validateWith = ParamValidator.class, description = "One or more parameters to pass to the worker, e.g. -param age=20 -param weight=\"200 kilos\"")
		private static List<String> params;

	}

	@Parameters
	private static class MainOptions {

		@Parameter(names = { "-help", "-h" }, hidden = true, help = true)
		private static boolean help;

		@Parameter(names = { "-env", "e" }, description = "The environment configuration to load")
		private static String environment = "production";
	}

	@Parameters(commandDescription = "Run a component")
	private static class RunComponentCommand {

		@Parameter(names = "-component", required = true, description = "The component to run ('service' or 'agent')", validateValueWith = RunComponentValidator.class)
		private static String component;
	}

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

	private static void enqueueJob(String workerType, List<String> params) throws APIException {

		// Load the environment configuration
		Config config = ConfigFactory.load(OnCue.MainOptions.environment);

		// Create the map of parameters
		Map<String, String> paramMap = new HashMap<String, String>();
		for (String param : params) {
			String[] components = param.split("=");
			paramMap.put(components[0], components[1]);
		}

		Job job = API.getInstance(config).enqueueJob(workerType, paramMap);
		System.out.println("Enqueued " + job);
		API.shutdown();
	}

	public static void main(String[] args) throws APIException {

		JCommander commander = new JCommander(new OnCue.MainOptions());
		commander.addCommand("run", new OnCue.RunComponentCommand());
		commander.addCommand("enqueue", new OnCue.EnqueueJobCommand());
		commander.parse(args);

		if (OnCue.MainOptions.help) {
			commander.usage();
			return;
		}

		switch (commander.getParsedCommand()) {
		case "run":
			runComponent(OnCue.RunComponentCommand.component);
			break;

		case "enqueue":
			enqueueJob(OnCue.EnqueueJobCommand.workerType, OnCue.EnqueueJobCommand.params);
			break;
		}
	}

	@SuppressWarnings("serial")
	private static void runComponent(String component) {

		// Load the environment configuration
		Config config = ConfigFactory.load(OnCue.MainOptions.environment);
		final ActorSystem system;
		final Settings settings;

		switch (component) {
		case "service":
			system = ActorSystem.create("oncue-service", config.getConfig("service").withFallback(config));
			settings = SettingsProvider.SettingsProvider.get(system);
			createServiceComponents(system, settings);
			break;

		case "agent":
			system = ActorSystem.create("oncue-agent", config.getConfig("client").withFallback(config));
			settings = SettingsProvider.SettingsProvider.get(system);
			system.actorOf(new Props(new UntypedActorFactory() {
				@Override
				public Actor create() throws Exception {
					return (Actor) Class.forName(settings.AGENT_CLASS).newInstance();
				}
			}), settings.AGENT_NAME);
			break;
		}
	}
}
