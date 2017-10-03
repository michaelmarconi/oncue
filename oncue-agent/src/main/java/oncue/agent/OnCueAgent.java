package oncue.agent;

import java.util.HashSet;
import java.util.Set;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.kernel.Bootable;
import oncue.common.settings.Settings;
import oncue.common.settings.SettingsProvider;

public class OnCueAgent implements Bootable {

	public static void main(String[] args) {
		new OnCueAgent().startup();
	}

	private ActorSystem system;

	@Override
	public void shutdown() {
		system.shutdown();
	}

	@SuppressWarnings("serial")
	@Override
	public void startup() {
		Config config = ConfigFactory.load();
		system = ActorSystem.create("oncue-agent", config.getConfig("oncue").withFallback(config));

		final Settings settings = SettingsProvider.SettingsProvider.get(system);
		final Set<String> workers = new HashSet<String>(config.getStringList("oncue.agent.workers"));

		// Fail fast if worker classes can't be instantiated
		try {
			system.actorOf(Props.create(Class.forName(settings.AGENT_CLASS), workers),
					settings.AGENT_NAME);
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}
	}
}
