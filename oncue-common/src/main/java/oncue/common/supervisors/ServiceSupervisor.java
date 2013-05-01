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
package oncue.common.supervisors;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;
import scala.concurrent.duration.Duration;
import akka.actor.ActorInitializationException;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.actor.SupervisorStrategy.Directive;
import akka.actor.SupervisorStrategyConfigurator;
import akka.japi.Function;

public class ServiceSupervisor implements SupervisorStrategyConfigurator {

	@Override
	public SupervisorStrategy create() {
		return new OneForOneStrategy(10, Duration.create("1 minute"), new Function<Throwable, Directive>() {

			@Override
			public Directive apply(Throwable t) throws Exception {
				
				if (t instanceof ActorInitializationException)
					return escalate();
				return restart();
			}
		});
	}
}
