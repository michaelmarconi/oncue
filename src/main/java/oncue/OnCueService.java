package oncue;

import oncue.settings.Settings;
import oncue.settings.SettingsProvider;
import akka.actor.Actor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.kernel.Bootable;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class OnCueService implements Bootable {

	private ActorSystem system;

	private Settings settings;

	private Config config;

	@SuppressWarnings("serial")
	@Override
	public void startup() {
		config = ConfigFactory.load();
		config = config.getConfig("service").withFallback(config);

		system = ActorSystem.create("oncue-service", config);
		settings = SettingsProvider.SettingsProvider.get(system);

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
				Class<?> schedulerClass = Class.forName(settings.SCHEDULER_CLASS);
				Class<?> backingStoreClass = null;
				if (settings.SCHEDULER_BACKING_STORE_CLASS != null)
					backingStoreClass = Class.forName(settings.SCHEDULER_BACKING_STORE_CLASS);
				return (Actor) schedulerClass.getConstructor(Class.class).newInstance(
						backingStoreClass);
			}
		}), settings.SCHEDULER_NAME);
	}

	@Override
	public void shutdown() {
		system.shutdown();
	}
}
