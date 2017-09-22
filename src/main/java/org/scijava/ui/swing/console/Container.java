package org.scijava.ui.swing.console;

import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This Container manages a list of items. Items can only be added to end of
 * the list. It's possible to add items, while iterating over the list.
 * Iterators never fail, and they will always be updated. Even if an element
 * is added after an iterator reached the end of the list,
 * {@link Iterator#hasNext()} will return true again, and
 * {@link Iterator#next()} will return the newly added element. This Container
 * is fully thread safe.
 *
 * @author Matthias Arzt
 */
// TODO merge into AbstractRecorder
public class Container<T> implements Iterable<T> {

	private final AtomicLong lastKey = new AtomicLong(0);

	private final NavigableMap<Long, T> map =
			new ConcurrentSkipListMap<>();

	private final Object lock = new Object();

	public Stream<T> stream() {
		Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(
				iterator(), Spliterator.ORDERED);
		return StreamSupport.stream(spliterator, /* parallel */ false);
	}

	public Iterator<T> iterator() {
		return new MyIterator();
	}

	public Iterator<T> iteratorAtEnd() {
		return new MyIterator(map.lastEntry());
	}

	long add(T value) {
		synchronized (lock) {
			long key = lastKey.incrementAndGet();
			map.put(key, value);
			return key;
		}
	}

	public void remove(long key) {
		map.remove(key);
	}

	public void clear() {
		map.clear();
	}

	private class MyIterator implements Iterator<T> {

		private Map.Entry<Long, T> entry;

		private Map.Entry<Long, T> nextEntry = null;

		public MyIterator() {
			this(null);
		}

		public MyIterator(Map.Entry<Long, T> entry) {
			this.entry = entry;
		}

		@Override
		public boolean hasNext() {
			nextEntry = (entry == null) ? map.firstEntry() : map.higherEntry(entry
					.getKey());
			return nextEntry != null;
		}

		@Override
		public T next() {
			if (nextEntry == null) if (!hasNext())
				throw new NoSuchElementException();
			entry = nextEntry;
			nextEntry = null;
			return entry.getValue();
		}
	}
}
