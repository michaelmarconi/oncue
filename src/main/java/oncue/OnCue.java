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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import oncue.api.APIException;
import oncue.api.AkkaAPI;
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
 */
public class OnCue {

	private enum Component {
		SERVICE {
			@Override
			public String toString() {
				return "service";
			}
		},
		AGENT {
			@Override
			public String toString() {
				return "agent";
			}
		}
	}

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

	@Parameters(commandDescription = "Run an agent")
	private static class RunAgentCommand {

		@Parameter(names = "-workers", required = true, description = "A comma-delimited list of worker types that can spawned by this agent")
		private static String workers;
	}

	@Parameters(commandDescription = "Run the service")
	private static class RunServiceCommand {

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

	private static void enqueueJob(String workerType, List<String> params) {

		// Load the environment configuration
		Config config = ConfigFactory.load(OnCue.MainOptions.environment);

		try {
			Job job;
			if (params != null) {

				// Create the map of parameters
				Map<String, String> paramMap = new HashMap<String, String>();
				for (String param : params) {
					String[] components = param.split("=");
					paramMap.put(components[0], components[1]);
				}
				job = AkkaAPI.getInstance(config).enqueueJob(workerType, paramMap);
			} else {
				job = AkkaAPI.getInstance(config).enqueueJob(workerType);
			}

			System.out.println("Enqueued " + job);

		} catch (APIException e) {
			System.err.println(e);
			AkkaAPI.shutdown();
			System.exit(-1);
		}
	}

	public static void main(String[] args) throws APIException {

		JCommander commander = new JCommander(new OnCue.MainOptions());
		commander.addCommand(Component.SERVICE.toString(), new OnCue.RunServiceCommand());
		commander.addCommand(Component.AGENT.toString(), new OnCue.RunAgentCommand());
		commander.addCommand("enqueue", new OnCue.EnqueueJobCommand());
		commander.parse(args);

		if (OnCue.MainOptions.help || commander.getParsedCommand() == null) {
			commander.usage();
			return;
		}

		switch (commander.getParsedCommand()) {
		case "service":
			runComponent(Component.SERVICE);
			break;

		case "agent":
			runComponent(Component.AGENT);
			break;

		case "enqueue":
			enqueueJob(OnCue.EnqueueJobCommand.workerType, OnCue.EnqueueJobCommand.params);
			break;
		}
	}

	@SuppressWarnings("serial")
	private static void runComponent(Component component) {

		// Load the environment configuration
		Config config = ConfigFactory.load(OnCue.MainOptions.environment);
		final ActorSystem system;
		final Settings settings;

		switch (component) {
		case SERVICE:
			system = ActorSystem.create("oncue-service", config.getConfig("service").withFallback(config));
			settings = SettingsProvider.SettingsProvider.get(system);
			createServiceComponents(system, settings);
			break;

		case AGENT:
			system = ActorSystem.create("oncue-agent", config.getConfig("client").withFallback(config));
			settings = SettingsProvider.SettingsProvider.get(system);
			system.actorOf(new Props(new UntypedActorFactory() {
				@Override
				public Actor create() throws Exception {
					return (Actor) Class.forName(settings.AGENT_CLASS).getConstructor(Collection.class)
							.newInstance(getWorkers());
				}
			}), settings.AGENT_NAME);
			break;
		}
	}

	/**
	 * @return the list of worker types that an agent can run
	 */
	private static Set<String> getWorkers() {
		return new HashSet<String>(Arrays.asList(RunAgentCommand.workers.split(",")));
	}
}
