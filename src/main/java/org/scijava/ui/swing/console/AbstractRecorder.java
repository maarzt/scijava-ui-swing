package org.scijava.ui.swing.console;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * @author Matthias Arzt
 */
public abstract class AbstractRecorder<T> {

	private final Container<T> recorded = new Container<>();

	private List<Runnable> observers = new CopyOnWriteArrayList<>();

	public void addObservers(Runnable observer) {
		observers.add(observer);
	}

	public void removeObserver(Runnable observer) {
		observers.remove(observer);
	}

	public Iterator<T> iterator() {
		return recorded.iterator();
	}

	public Stream<T> stream() {
		return recorded.stream();
	}

	/**
	 * Same as {@link #iterator()}, but the Iterator will only return log messages
	 * and text recorded after the iterator has been created.
	 */
	public Iterator<T> iteratorAtEnd() {
		return recorded.iteratorAtEnd();
	}

	public void clear() {
		recorded.clear();
	}

	public void remove(long key) {
		recorded.remove(key);
	}

	public long add(T item) {
		return recorded.add(item);
	}

	public void notifyListeners() {
		for (Runnable listener : observers)
			listener.run();
	}
}
