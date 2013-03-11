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

import static akka.pattern.Patterns.ask;

import java.util.Map;

import oncue.messages.internal.EnqueueJob;
import oncue.messages.internal.Job;
import oncue.settings.Settings;
import oncue.settings.SettingsProvider;
import scala.concurrent.Await;
import akka.actor.ActorSystem;
import akka.actor.TypedActor;
import akka.actor.TypedProps;
import akka.util.Timeout;

import com.typesafe.config.Config;

/**
 * An implementation of the {@linkplain API} that relies on Akka remoting to
 * connect to an onCue service directly. This class is typed actor and acts as a
 * bridge into Actor-space.
 */
public class AkkaAPI implements API {

	// The application settings
	private static Settings settings;

	// The singleton instance
	private static API instance;

	// The Actor system
	private static ActorSystem system;

	/**
	 * Get the API singleton instance
	 * 
	 * @param config
	 *            is the {@linkplain Config}uration to use when making the
	 *            instance
	 * @return an API instance that implements the {@linkplain API} interface
	 */
	public static API getInstance(Config config) {
		if (instance == null) {
			system = ActorSystem.create("oncue-api", config.getConfig("client").withFallback(config));
			settings = SettingsProvider.SettingsProvider.get(system);
			instance = TypedActor.get(system).typedActorOf(new TypedProps<AkkaAPI>(API.class, AkkaAPI.class),
					settings.API_NAME);
		}
		return instance;
	}

	/**
	 * Shut down the API. This will wait for all actors to finish their work and
	 * ensure that the process terminates cleanly when the actor system
	 * terminates.
	 */
	public static void shutdown() {
		system.shutdown();
		while (!system.isTerminated()) {
			try {
				Thread.sleep(100);
				Thread.yield();
			} catch (InterruptedException e) {
				// Ignore this!
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see oncue.api.API#enqueueJob(java.lang.String)
	 */
	@Override
	public Job enqueueJob(String workerType) throws APIException {
		return enqueueJob(workerType, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see oncue.api.API#enqueueJob(java.lang.String, java.util.Map)
	 */
	@Override
	public Job enqueueJob(String workerType, Map<String, Object> jobParams) throws APIException {
		try {
			EnqueueJob enqueueJobMessage = new EnqueueJob(workerType, jobParams);

			return (Job) Await.result(
					ask(TypedActor.context().actorFor(settings.QUEUE_MANAGER_PATH), enqueueJobMessage, new Timeout(
							settings.API_TIMEOUT)), settings.API_TIMEOUT);
		} catch (Exception e) {
			if (e instanceof akka.pattern.AskTimeoutException)
				throw new APIException("Failed to reach the Queue Manager. Is it running?");
			else
				throw new APIException(e);
		}
	}
}
