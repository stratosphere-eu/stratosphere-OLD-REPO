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

package eu.stratosphere.pact.runtime.resettable;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.stratosphere.nephele.services.memorymanager.MemoryManager;
import eu.stratosphere.nephele.services.memorymanager.spi.DefaultMemoryManager;
import eu.stratosphere.nephele.template.AbstractInvokable;
import eu.stratosphere.pact.common.type.PactRecord;
import eu.stratosphere.pact.common.type.base.PactInteger;
import eu.stratosphere.pact.runtime.resettable.BlockResettableMutableObjectIterator;
import eu.stratosphere.pact.runtime.test.util.DummyInvokable;
import eu.stratosphere.pact.runtime.test.util.MutableObjectIteratorWrapper;
import eu.stratosphere.pact.runtime.util.MutableObjectIterator;
import junit.framework.Assert;

public class BlockResettableMutableObjectIteratorTest
{
	private static final int MEMORY_CAPACITY = 100000;
	
	private static final int NUM_VALUES = 20000;
	
	private MemoryManager memman;

	private MutableObjectIterator<PactRecord> reader;

	private List<PactRecord> objects;

	@Before
	public void startup() {
		// set up IO and memory manager
		this.memman = new DefaultMemoryManager(MEMORY_CAPACITY);
		
		// create test objects
		this.objects = new ArrayList<PactRecord>(20000);
		for (int i = 0; i < NUM_VALUES; ++i) {
			this.objects.add(new PactRecord(new PactInteger(i)));
		}
		
		// create the reader
		this.reader = new MutableObjectIteratorWrapper(objects.iterator());
	}
	
	@After
	public void shutdown() {
		this.objects = null;
		
		// check that the memory manager got all segments back
		if (!this.memman.verifyEmpty()) {
			Assert.fail("A memory leak has occurred: Not all memory was properly returned to the memory manager.");
		}
		
		this.memman.shutdown();
		this.memman = null;
	}

	@Test
	public void testSerialBlockResettableIterator() throws Exception
	{
		final AbstractInvokable memOwner = new DummyInvokable();
		// create the resettable Iterator
		BlockResettableMutableObjectIterator iterator = new BlockResettableMutableObjectIterator(memman, reader, BlockResettableMutableObjectIterator.MIN_BUFFER_SIZE, 1, memOwner);
		// open the iterator
		iterator.open();
		
		final PactRecord target = new PactRecord();
		
		// now test walking through the iterator
		int lower = 0;
		int upper = 0;
		do {
			lower = upper;
			upper = lower;
			// find the upper bound
			while (iterator.next(target)) {
				int val = target.getField(0, PactInteger.class).getValue();
				Assert.assertEquals(upper++, val);
			}
			// now reset the buffer a few times
			for (int i = 0; i < 5; ++i) {
				iterator.reset();
				int count = 0;
				while (iterator.next(target)) {
					int val = target.getField(0, PactInteger.class).getValue();
					Assert.assertEquals(lower + (count++), val);
				}
				Assert.assertEquals(upper - lower, count);
			}
		} while (iterator.nextBlock());
		Assert.assertEquals(NUM_VALUES, upper);
		// close the iterator
		iterator.close();
	}

	@Test
	public void testDoubleBufferedBlockResettableIterator() throws Exception
	{
		final AbstractInvokable memOwner = new DummyInvokable();
		// create the resettable Iterator
		BlockResettableMutableObjectIterator iterator = new BlockResettableMutableObjectIterator(memman, reader,2 * BlockResettableMutableObjectIterator.MIN_BUFFER_SIZE, 2, memOwner);
		// open the iterator
		iterator.open();
		
		PactRecord target = new PactRecord();
		
		// now test walking through the iterator
		int lower = 0;
		int upper = 0;
		do {
			lower = upper;
			upper = lower;
			// find the upper bound
			while (iterator.next(target)) {
				int val = target.getField(0, PactInteger.class).getValue();
				Assert.assertEquals(upper++, val);
			}
			// now reset the buffer a few times
			for (int i = 0; i < 5; ++i) {
				iterator.reset();
				int count = 0;
				while (iterator.next(target)) {
					int val = target.getField(0, PactInteger.class).getValue();
					Assert.assertEquals(lower + (count++), val);
				}
				Assert.assertEquals(upper - lower, count);
			}
		} while (iterator.nextBlock());
		Assert.assertEquals(NUM_VALUES, upper);
		
		// close the iterator
		iterator.close();
	}

	@Test
	public void testTripleBufferedBlockResettableIterator() throws Exception
	{
		final AbstractInvokable memOwner = new DummyInvokable();
		// create the resettable Iterator
		BlockResettableMutableObjectIterator iterator = new BlockResettableMutableObjectIterator(memman, reader, 3 * BlockResettableMutableObjectIterator.MIN_BUFFER_SIZE, 3, memOwner);
		// open the iterator
		iterator.open();
		
		PactRecord target = new PactRecord();
		
		// now test walking through the iterator
		int lower = 0;
		int upper = 0;
		do {
			lower = upper;
			upper = lower;
			// find the upper bound
			while (iterator.next(target)) {
				int val = target.getField(0, PactInteger.class).getValue();
				Assert.assertEquals(upper++, val);
			}
			// now reset the buffer a few times
			for (int i = 0; i < 5; ++i) {
				iterator.reset();
				int count = 0;
				while (iterator.next(target)) {
					int val = target.getField(0, PactInteger.class).getValue();
					Assert.assertEquals(lower + (count++), val);
				}
				Assert.assertEquals(upper - lower, count);
			}
		} while (iterator.nextBlock());
		Assert.assertEquals(NUM_VALUES, upper);
		
		// close the iterator
		iterator.close();
	}

}