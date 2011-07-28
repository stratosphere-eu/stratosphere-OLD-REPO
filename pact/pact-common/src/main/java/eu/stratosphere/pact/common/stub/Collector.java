/***********************************************************************************************************************
 *
 * Copyright (C) 2010 by the Stratosphere project (http://stratosphere.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 **********************************************************************************************************************/

package eu.stratosphere.pact.common.stub;

import eu.stratosphere.pact.common.type.Key;
import eu.stratosphere.pact.common.type.Value;

/**
 * Collects the output of PACT first-order user function implemented as {@link eu.stratosphere.pact.common.stub.Stub}.
 * The collected data is forwards to the next contract.
 * 
 * @author Erik Nijkamp
 * @author Fabian Hueske
 *
 * @param <K>
 * @param <V>
 */
public interface Collector<K extends Key, V extends Value> {
	
	/**
	 * An emitted key and an emitted value of a PACT first-order user function implemented as {@link eu.stratosphere.pact.common.stub.Stub}.
	 * 
	 * @param key The emitted key.
	 * @param value The emitted value.
	 */
	void collect(K key, V value);

	/**
	 * Closes the collector.
	 */
	void close();
}