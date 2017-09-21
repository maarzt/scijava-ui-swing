package org.scijava.ui.swing.console;

import org.scijava.console.OutputEvent;
import org.scijava.console.OutputListener;

import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * @author Matthias Arzt
 */
public class ConsoleRecorder implements OutputListener {

	private final Container<ItemTextPane.Item> container = new Container<>();

	private List<Runnable> observers = new CopyOnWriteArrayList<>();

	public void addObservers(Runnable observer) {
		observers.add(observer);
	}

	public void removeObserver(Runnable observer) {
		observers.remove(observer);
	}

	private void notifyListeners() {
		for (Runnable listener : observers)
			listener.run();
	}

	public Stream<ItemTextPane.Item> stream() {
		return container.stream();
	}

	private final AttributeSet defaultAttributes = new SimpleAttributeSet();

	@Override
	public void outputOccurred(OutputEvent event) {
		container.add(new ItemTextPane.Item(defaultAttributes, event.getOutput()));
		notifyListeners();
	}

	private class StreamAdapter extends ByteArrayOutputStream {

		private final AttributeSet attributes;

		private StreamAdapter(AttributeSet attributes) {
			this.attributes = attributes;
		}

		@Override
		public void flush() {
			String text = toString();
			if (text.isEmpty()) return;
			splitToLines(text);
			reset();
		}

		public void splitToLines(String s) {
			int indexBefore = 0;
			while (true) {
				int index = s.indexOf("\n", indexBefore) + 1;
				if (index == 0) break;
				addChunk(s.substring(indexBefore, index));
				indexBefore = index;
			}
			addChunk(s.substring(indexBefore));
		}

		private void addChunk(String substring) {
			container.add(new ItemTextPane.Item(attributes, substring));
		}
	}
}
