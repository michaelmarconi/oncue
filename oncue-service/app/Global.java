import oncue.common.settings.Settings;
import oncue.common.settings.SettingsProvider;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.libs.Akka;
import actors.EventMachine;
import akka.actor.Actor;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;

public class Global extends GlobalSettings {

	@Override
	@SuppressWarnings("serial")
	public void onStart(Application app) {
		
		Logger.info("onCue service starting...");

		final Settings settings = SettingsProvider.SettingsProvider.get(Akka.system());

		// Start the queue manager
		Akka.system().actorOf(new Props(new UntypedActorFactory() {
			@Override
			public Actor create() throws Exception {
				return (Actor) Class.forName(settings.QUEUE_MANAGER_CLASS).newInstance();
			}
		}), settings.QUEUE_MANAGER_NAME);
		
		// Start the scheduler
		Akka.system().actorOf(new Props(new UntypedActorFactory() {
			@Override
			public Actor create() throws Exception {
				Class<?> schedulerClass = Class.forName(settings.SCHEDULER_CLASS);
				Class<?> backingStoreClass = null;
				if (settings.SCHEDULER_BACKING_STORE_CLASS != null)
					backingStoreClass = Class.forName(settings.SCHEDULER_BACKING_STORE_CLASS);
				return (Actor) schedulerClass.getConstructor(Class.class).newInstance(backingStoreClass);
			}
		}), settings.SCHEDULER_NAME);

		// Start the event stream listener
		Akka.system().actorOf(new Props(EventMachine.class), "event-stream-listener");
	}

	@Override
	public void onStop(Application app) {
		Logger.info("onCue service shutdown.");
	}
}
