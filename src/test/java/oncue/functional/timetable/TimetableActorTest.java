package oncue.functional.timetable;

import oncue.settings.Settings;
import oncue.settings.SettingsProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.testkit.JavaTestKit;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TimetableActorTest {

	protected Config config;
	protected ActorSystem system;
	protected Settings settings;
	protected LoggingAdapter log;

	@Before
	public void startActorSystem() {
		config = ConfigFactory.load("timetable-test");
		system = ActorSystem.create("oncue-test", config);
		settings = SettingsProvider.SettingsProvider.get(system);
		log = Logging.getLogger(system, this);
	}

	@After
	public void stopActorSystem() throws Exception {
		system.shutdown();
		while (!system.isTerminated()) {
			log.debug("Waiting for system to shut down...");
			Thread.sleep(500);
		}
		log.debug("System shut down");
	}

	@Test
	public void test() {
		new JavaTestKit(system) {
			{
				expectNoMsg(duration("5 seconds"));
			}
		};
	}

}
