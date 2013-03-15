package oncue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import oncue.settings.Settings;
import oncue.settings.SettingsProvider;
import akka.actor.Actor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.kernel.Bootable;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class BootOnCueAgent implements Bootable {

	private ActorSystem system;

	private Settings settings;

	private Config config;

	@SuppressWarnings("serial")
	@Override
	public void startup() {
		config = ConfigFactory.load();
		config = config.getConfig("client").withFallback(config);

		final Set<String> workers = new HashSet<String>(config.getStringList("workers"));

		system = ActorSystem.create("oncue-agent", config);
		settings = SettingsProvider.SettingsProvider.get(system);

		system.actorOf(new Props(new UntypedActorFactory() {

			@Override
			public Actor create() throws Exception {
				return (Actor) Class.forName(settings.AGENT_CLASS).getConstructor(Collection.class)
						.newInstance(workers);
			}
		}), settings.AGENT_NAME);
	}

	@Override
	public void shutdown() {
		system.shutdown();
	}
}
