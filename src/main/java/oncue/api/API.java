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
 * The implementation of the {@linkplain APIContract} interface. This class is
 * typed actor and acts as a bridge into Actor-space.
 */
public class API implements APIContract {

	// The application settings
	private static Settings settings;

	// The singleton instance
	private static APIContract instance;

	// The Actor system
	private static ActorSystem system;

	/**
	 * Get the API singleton instance
	 * 
	 * @param config
	 *            is the {@linkplain Config}uration to use when making the
	 *            instance
	 * @return an API instance that implements the {@linkplain APIContract}
	 *         interface
	 */
	public static APIContract getInstance(Config config) {
		if (instance == null) {
			system = ActorSystem.create("oncue-api", config.getConfig("client").withFallback(config));
			settings = SettingsProvider.SettingsProvider.get(system);
			instance = TypedActor.get(system).typedActorOf(new TypedProps<API>(APIContract.class, API.class),
					settings.API_NAME);
		}
		return instance;
	}

	public static void shutdown() {
		TypedActor.get(system).stop(instance);
		system.shutdown();
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
	public Job enqueueJob(String workerType, Map<String, String> jobParams) throws APIException {
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
