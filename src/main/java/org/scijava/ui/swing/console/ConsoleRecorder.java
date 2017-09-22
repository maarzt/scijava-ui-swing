/*
 * #%L
 * SciJava Common shared library for SciJava software.
 * %%
 * Copyright (C) 2009 - 2017 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package org.scijava.ui.swing.console;

import org.scijava.console.OutputEvent;
import org.scijava.console.OutputListener;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * @author Matthias Arzt
 */
public class ConsoleRecorder extends AbstractRecorder<ItemTextPane.Item> implements OutputListener
{
	private final PrintStream outStream = printStream(normal(Color.black));
	private final PrintStream errStream = printStream(normal(Color.red));
	private final PrintStream globalOutStream = printStream(italic(Color.black));
	private final PrintStream globalErrStream = printStream(italic(Color.red));


	public PrintStream printStream(AttributeSet tag) {
		return new PrintStream(new StreamAdapter(tag), true);
	}

	// -- OutputListener methods --

	@Override
	public void outputOccurred(OutputEvent event) {
		PrintStream stream = (event.getSource() == OutputEvent.Source.STDOUT)
				? (event.isContextual() ? outStream : globalOutStream)
				: (event.isContextual() ? errStream : globalErrStream);
		stream.append(event.getOutput());
	}

	// -- Helper methods --

	private static MutableAttributeSet normal(Color color) {
		MutableAttributeSet style = new SimpleAttributeSet();
		StyleConstants.setForeground(style, color);
		return style;
	}

	private static MutableAttributeSet italic(Color color) {
		MutableAttributeSet style = normal(color);
		StyleConstants.setItalic(style, true);
		return style;
	}

	// -- Helper classes --

	private class StreamAdapter extends ByteArrayOutputStream {

		private final AttributeSet tag;

		long lastKey = -1;

		String remainder = "";

		private StreamAdapter(AttributeSet tag) {
			this.tag = new SimpleAttributeSet(tag);
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
				completeLine(remainder + s.substring(indexBefore, index));
				remainder = "";
				indexBefore = index;
			}
			remainder = remainder + s.substring(indexBefore);
			if (!remainder.isEmpty()) incompleteLine(remainder);
		}

		private void incompleteLine(String line) {
			if (lastKey >= 0) remove(lastKey);
			lastKey = add(new ItemTextPane.Item(tag, line, tag, false));
			notifyListeners();
		}

		private void completeLine(String line) {
			if (lastKey >= 0) remove(lastKey);
			lastKey = -1;
			add(new ItemTextPane.Item(tag, line, tag, true));
			notifyListeners();
		}
	}
}
