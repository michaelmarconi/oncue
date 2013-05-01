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
package oncue.tests.base;

import java.util.Set;

import oncue.agent.AbstractAgent;
import oncue.common.settings.Settings;
import oncue.common.settings.SettingsProvider;
import oncue.queuemanager.AbstractQueueManager;
import oncue.scheduler.AbstractScheduler;

import org.junit.After;
import org.junit.Before;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public abstract class ActorSystemTest {

	protected Config config;
	protected LoggingAdapter log;
	protected Settings settings;
	protected ActorSystem system;

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
	 * @param probe
	 *            can be null
	 */
	@SuppressWarnings("serial")
	public ActorRef createAgent(ActorSystem system, final Set<String> workers, final ActorRef probe) {
		agentCount++;
		return system.actorOf(new Props(new UntypedActorFactory() {
			@Override
			public Actor create() throws Exception {
				AbstractAgent agent = (AbstractAgent) Class.forName(settings.AGENT_CLASS).getConstructor(Set.class)
						.newInstance(workers);
				if (probe != null)
					agent.injectProbe(probe);
				return agent;
			}
		}), settings.AGENT_NAME + agentCount);
	}

	/**
	 * Construct a Queue Manager without a probe
	 */
	public ActorRef createQueueManager(ActorSystem system) {
		return createQueueManager(system, null);
	}

	/**
	 * Create a queue manager component, with an optional probe
	 * 
	 * @param probe
	 *            can be null
	 */
	@SuppressWarnings("serial")
	public ActorRef createQueueManager(ActorSystem system, final ActorRef probe) {
		return system.actorOf(new Props(new UntypedActorFactory() {
			@Override
			public Actor create() throws Exception {
				AbstractQueueManager queueManager = (AbstractQueueManager) Class.forName(settings.QUEUE_MANAGER_CLASS)
						.newInstance();
				if (probe != null)
					queueManager.injectProbe(probe);
				return queueManager;
			}
		}), settings.QUEUE_MANAGER_NAME);
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
	 * @param probe
	 *            can be null
	 */
	@SuppressWarnings("serial")
	public ActorRef createScheduler(ActorSystem system, final ActorRef probe) {
		return system.actorOf(new Props(new UntypedActorFactory() {
			@Override
			public Actor create() throws Exception {
				Class<?> schedulerClass = Class.forName(settings.SCHEDULER_CLASS);
				Class<?> backingStoreClass = null;
				if (settings.SCHEDULER_BACKING_STORE_CLASS != null)
					backingStoreClass = Class.forName(settings.SCHEDULER_BACKING_STORE_CLASS);

				@SuppressWarnings("rawtypes")
				AbstractScheduler scheduler = (AbstractScheduler) schedulerClass.getConstructor(Class.class)
						.newInstance(backingStoreClass);
				if (probe != null)
					scheduler.injectProbe(probe);
				return scheduler;
			}
		}), settings.SCHEDULER_NAME);
	}

	@Before
	public void startActorSystem() {
		/*
		 * Load configuration specific to this test and fall back to the
		 * reference configuration
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
		while (!system.isTerminated()) {
			log.debug("Waiting for system to shut down...");
			Thread.sleep(500);
		}
		log.debug("System shut down");
	}

}
