package oncue.service;

import oncue.common.settings.Settings;
import oncue.common.settings.SettingsProvider;
import akka.actor.Actor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.kernel.Bootable;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Invoke this class using the Akka microkernel to bring up the onCue service
 * components.
 */
public class OnCueService implements Bootable {

	public static void main(String[] args) {
		new OnCueService().startup();
	}

	private Config config;

	private Settings settings;
	
	private ActorSystem system;

	@Override
	public void shutdown() {
		system.shutdown();
	}

	@SuppressWarnings("serial")
	@Override
	public void startup() {
		config = ConfigFactory.load();
		config = config.getConfig("oncue-service").withFallback(config);
		
		system = ActorSystem.create("oncue-service", config);
		settings = SettingsProvider.SettingsProvider.get(system);

		// Start the queue manager
		system.actorOf(new Props(new UntypedActorFactory() {
			@Override
			public Actor create() throws Exception {
				return (Actor) Class.forName(settings.QUEUE_MANAGER_CLASS)
						.newInstance();
			}
		}), settings.QUEUE_MANAGER_NAME);

		// Start the scheduler
		system.actorOf(new Props(new UntypedActorFactory() {
			@Override
			public Actor create() throws Exception {
				Class<?> schedulerClass = Class
						.forName(settings.SCHEDULER_CLASS);
				Class<?> backingStoreClass = null;
				if (settings.SCHEDULER_BACKING_STORE_CLASS != null)
					backingStoreClass = Class
							.forName(settings.SCHEDULER_BACKING_STORE_CLASS);
				return (Actor) schedulerClass.getConstructor(Class.class)
						.newInstance(backingStoreClass);
			}
		}), settings.SCHEDULER_NAME);
		
		// Start the RESTful API
		system.actorOf(new Props(RESTfulAPI.class), "restful-api");
	}
}
