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
	 * @param probe
	 *            can be null
	 */
	@SuppressWarnings("serial")
	public ActorRef createAgent(final Set<String> workers, final ActorRef probe) {
		return agentSystem.actorOf(new Props(new UntypedActorFactory() {
			@Override
			public Actor create() throws Exception {
				AbstractAgent agent = (AbstractAgent) Class.forName(agentSettings.AGENT_CLASS)
						.getConstructor(Set.class).newInstance(workers);
				if (probe != null)
					agent.injectProbe(probe);
				return agent;
			}
		}), agentSettings.AGENT_NAME);
	}

	/**
	 * Create a queue manager component, with an optional probe
	 * 
	 * @param probe
	 *            can be null
	 */
	@SuppressWarnings("serial")
	public ActorRef createQueueManager(final ActorRef probe) {
		return serviceSystem.actorOf(new Props(new UntypedActorFactory() {
			@Override
			public Actor create() throws Exception {
				AbstractQueueManager queueManager = (AbstractQueueManager) Class.forName(
						serviceSettings.QUEUE_MANAGER_CLASS).newInstance();
				if (probe != null)
					queueManager.injectProbe(probe);
				return queueManager;
			}
		}), serviceSettings.QUEUE_MANAGER_NAME);
	}

	/**
	 * Create a scheduler component, with an optional probe
	 * 
	 * @param probe
	 *            can be null
	 */
	@SuppressWarnings("serial")
	public ActorRef createScheduler(final ActorRef probe) {
		return serviceSystem.actorOf(new Props(new UntypedActorFactory() {
			@Override
			public Actor create() throws Exception {
				Class<?> schedulerClass = Class.forName(serviceSettings.SCHEDULER_CLASS);
				Class<?> backingStoreClass = null;
				if (serviceSettings.SCHEDULER_BACKING_STORE_CLASS != null)
					backingStoreClass = Class.forName(serviceSettings.SCHEDULER_BACKING_STORE_CLASS);

				@SuppressWarnings("rawtypes")
				AbstractScheduler scheduler = (AbstractScheduler) schedulerClass.getConstructor(Class.class)
						.newInstance(backingStoreClass);
				if (probe != null)
					scheduler.injectProbe(probe);
				return scheduler;
			}
		}), serviceSettings.SCHEDULER_NAME);
	}

	@Before
	public void startActorSystems() {
		/*
		 * Load configuration specific to this test and fall back to the
		 * reference configuration
		 */
		serviceConfig = ConfigFactory.load();
		serviceConfig = ConfigFactory.load(getClass().getSimpleName() + "-Service").withFallback(serviceConfig);

		agentConfig = ConfigFactory.load();
		agentConfig = ConfigFactory.load(getClass().getSimpleName() + "-Agent").withFallback(agentConfig);

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

}
