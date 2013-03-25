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
package oncue.backingstore;

import oncue.common.settings.Settings;
import akka.actor.ActorSystem;

/**
 * Extend this class to create a new persistent backing store.
 */
public abstract class AbstractBackingStore implements BackingStore {

	protected Settings settings;
	protected ActorSystem system;

	public AbstractBackingStore(ActorSystem system, Settings settings) {
		this.system = system;
		this.settings = settings;
	}

}
