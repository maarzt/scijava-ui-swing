package org.scijava.ui.swing.console;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Matthias Arzt
 */
public class Container<T> implements Iterable<T> {

	private final AtomicLong lastKey = new AtomicLong(0);

	private final Map<Long, T> map = new ConcurrentHashMap<>();

	public Stream<T> stream() {
		Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(
				iterator(), Spliterator.ORDERED);
		return StreamSupport.stream(spliterator, /* parallel */ false);
	}

	public Iterator<T> iterator() {
		return new MyIterator();
	}

	public Iterator<T> iteratorAtEnd() {
		return new MyIterator(lastKey.get());
	}

	public long add(T value) {
		long key = lastKey.getAndIncrement();
		map.put(key, value);
		return key;
	}

	public void clear() {
		map.clear();
	}

	private class MyIterator implements Iterator<T> {

		private long nextIndex = 0;

		public MyIterator() {
			this(0);
		}

		public MyIterator(long nextIndex) {
			this.nextIndex = nextIndex;
		}

		@Override
		public boolean hasNext() {
			return map.containsKey(nextIndex);
		}

		@Override
		public T next() {
			T value = map.get(nextIndex);
			if(value == null)
				throw new NoSuchElementException();
			nextIndex++;
			return value;
		}
	}

}
