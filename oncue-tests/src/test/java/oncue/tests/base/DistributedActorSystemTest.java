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
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import oncue.agent.AbstractAgent;
import oncue.backingstore.RedisBackingStore;
import oncue.backingstore.RedisBackingStore.RedisConnection;
import oncue.common.settings.Settings;
import oncue.common.settings.SettingsProvider;
import oncue.scheduler.AbstractScheduler;
import oncue.tests.Creators;

public abstract class DistributedActorSystemTest {

	protected Config serviceConfig;

	protected ActorSystem serviceSystem;

	protected Settings serviceSettings;

	protected LoggingAdapter serviceLog;

	protected Config agentConfig;

	protected ActorSystem agentSystem;

	protected Settings agentSettings;

	protected LoggingAdapter agentLog;

	/**
	 * Create an agent component, with a set of workers and an optional probe
	 * 
	 * @param probe can be null
	 */
	@SuppressWarnings({"serial", "unchecked"})
	public ActorRef createAgent(final Set<String> workers, final ActorRef probe) {
		Class<AbstractAgent> agentClass = null;
		try {
			agentClass = (Class<AbstractAgent>) Class.forName(agentSettings.AGENT_CLASS);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return agentSystem.actorOf(
				Creators.makeProps(probe, agentClass, workers), agentSettings.AGENT_NAME);
	}

	/**
	 * Create a scheduler component, with an optional probe
	 * 
	 * @param probe can be null
	 */
	@SuppressWarnings({"serial", "unchecked"})
	public ActorRef createScheduler(final ActorRef probe) {

		try {
			Class<AbstractScheduler> schedulerClass = (Class<AbstractScheduler>)
					Class.forName(serviceSettings.SCHEDULER_CLASS);
			Class<?> backingStoreClass = null;
			if (serviceSettings.SCHEDULER_BACKING_STORE_CLASS != null)
				backingStoreClass = Class
						.forName(serviceSettings.SCHEDULER_BACKING_STORE_CLASS);

			return serviceSystem.actorOf(
					Creators.makeProps(probe, schedulerClass, backingStoreClass),
					serviceSettings.SCHEDULER_NAME);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Before
	public void startActorSystems() {
		/*
		 * Load configuration specific to this test and fall back to the reference configuration
		 */
		serviceConfig = ConfigFactory.load();
		serviceConfig = ConfigFactory.load(getClass().getSimpleName() + "-Service")
				.withFallback(serviceConfig);

		agentConfig = ConfigFactory.load();
		agentConfig = ConfigFactory.load(getClass().getSimpleName() + "-Agent")
				.withFallback(agentConfig);

		serviceSystem = ActorSystem.create("oncue-service", serviceConfig);
		serviceSettings = SettingsProvider.SettingsProvider.get(serviceSystem);
		serviceLog = Logging.getLogger(serviceSystem, this);

		agentSystem = ActorSystem.create("oncue-agent", agentConfig);
		agentSettings = SettingsProvider.SettingsProvider.get(agentSystem);
		agentLog = Logging.getLogger(agentSystem, this);
	}

	@After
	public void stopActorSystems() throws Exception {
		serviceSystem.shutdown();
		agentSystem.shutdown();
		while (!serviceSystem.isTerminated() || !agentSystem.isTerminated()) {
			serviceLog.info("Waiting for systems to shut down...");
			Thread.sleep(500);
		}
		serviceLog.debug("Systems shut down");
	}

	@Before
	@After
	public void cleanRedis() {
		try (RedisConnection redis = new RedisBackingStore.RedisConnection()) {
			redis.flushDB();
		}
	}
}
