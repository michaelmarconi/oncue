/*******************************************************************************
 * Copyright 2013 Michael Marconi
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package oncue.tests.base;

import java.util.Set;

import org.junit.After;
import org.junit.Before;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import oncue.agent.AbstractAgent;
import oncue.backingstore.RedisBackingStore.RedisConnection;
import oncue.common.settings.Settings;
import oncue.common.settings.SettingsProvider;
import oncue.scheduler.AbstractScheduler;
import oncue.tests.Creators;

public abstract class ActorSystemTest {

	protected Config config;

	protected LoggingAdapter log;

	protected Settings settings;

	protected ActorSystem system;

	// By default ActorSystemTest children will wait for all actors to finish before shutting down
	// the test system. If you want to shut down the test system while e.g. workers are still
	// running, set this to false.
	protected boolean waitForRunningJobs = true;

	// Required for naming agents uniquely
	private int agentCount = 0;

	/**
	 * Construct a Agent without a probe
	 */
	public ActorRef createAgent(ActorSystem system, final Set<String> workers) {
		return createAgent(system, workers, null);
	}

	/**
	 * Create an agent component, with a set of workers and an optional probe
	 * 
	 * @param probe can be null
	 */
	@SuppressWarnings({"serial", "unchecked"})
	public ActorRef createAgent(ActorSystem system, final Set<String> workers,
			final ActorRef probe) {
		agentCount++;
		Class<AbstractAgent> agentClass = null;
		try {
			agentClass = (Class<AbstractAgent>)
					Class.forName(settings.AGENT_CLASS);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		return system.actorOf(Creators.makeProps(probe, agentClass, workers),
				settings.AGENT_NAME + agentCount);
	}

	/**
	 * Construct a Scheduler without a probe
	 */
	public ActorRef createScheduler(ActorSystem system) {
		return createScheduler(system, null);
	}

	/**
	 * Create a scheduler component, with an optional probe
	 * 
	 * @param probe can be null
	 */
	@SuppressWarnings({"serial", "unchecked"})
	public ActorRef createScheduler(ActorSystem system, final ActorRef probe) {
		Class<AbstractScheduler> schedulerClass;
		Class<?> backingStoreClass = null;
		try {
			schedulerClass = (Class<AbstractScheduler>)
					Class.forName(settings.SCHEDULER_CLASS);
			if (settings.SCHEDULER_BACKING_STORE_CLASS != null)
				backingStoreClass = Class.forName(settings.SCHEDULER_BACKING_STORE_CLASS);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}

		return system.actorOf(Creators.makeProps(probe, schedulerClass, backingStoreClass),
				settings.SCHEDULER_NAME);
	}

	@Before
	public void startActorSystem() {
		/*
		 * Load configuration specific to this test and fall back to the reference configuration
		 */
		config = ConfigFactory.load();
		config = ConfigFactory.load(getClass().getSimpleName()).withFallback(config);

		system = ActorSystem.create("oncue-test", config);
		settings = SettingsProvider.SettingsProvider.get(system);
		log = Logging.getLogger(system, this);
	}

	@After
	public void stopActorSystem() throws Exception {
		system.shutdown();
		if (waitForRunningJobs) {
			while (!system.isTerminated()) {
				log.debug("Waiting for system to shut down...");
				Thread.sleep(500);
			}
		}
		log.debug("System shut down");
	}

	@Before
	@After
	public void cleanRedis() {
		try (RedisConnection redis = new RedisConnection()) {
			redis.flushDB();
		}
	}
}
