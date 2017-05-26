/*
 * #%L
 * SciJava UI components for Java Swing.
 * %%
 * Copyright (C) 2010 - 2017 Board of Regents of the University of
 * Wisconsin-Madison.
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

import java.awt.*;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import net.miginfocom.swing.MigLayout;

import org.scijava.Context;
import org.scijava.console.OutputEvent;
import org.scijava.console.OutputListener;
import org.scijava.log.DefaultLogFormatter;
import org.scijava.log.IgnoreAsCallingClass;
import org.scijava.log.LogFormatter;
import org.scijava.log.LogLevel;
import org.scijava.log.LogListener;
import org.scijava.log.LogMessage;
import org.scijava.log.LogRecorder;
import org.scijava.log.LogService;
import org.scijava.log.LogSource;
import org.scijava.log.Logger;
import org.scijava.thread.ThreadService;

/**
 * {@link LoggingPanel} can display log message and console output as a list,
 * and provides convenient ways for the user to filter this list. LoggingPanel
 * implements {@link LogListener} and {@link OutputListener}, that way it can
 * receive log message and console output from {@link LogService},
 * {@link org.scijava.log.ListenableLogger} and
 * {@link org.scijava.console.ConsoleService}
 *
 * @see LogService
 * @see Logger
 * @see org.scijava.log.ListenableLogger
 * @see org.scijava.console.ConsoleService
 * @author Matthias Arzt
 */
@IgnoreAsCallingClass
public class LoggingPanel extends JPanel implements LogListener,
	OutputListener
{

	private static final AttributeSet DEFAULT_STYLE = new SimpleAttributeSet();
	private static final AttributeSet STYLE_ERROR = normal(new Color(200, 0, 0));
	private static final AttributeSet STYLE_WARN = normal(new Color(200, 140, 0));
	private static final AttributeSet STYLE_INFO = normal(Color.BLACK);
	private static final AttributeSet STYLE_DEBUG = normal(new Color(0, 0, 200));
	private static final AttributeSet STYLE_TRACE = normal(Color.GRAY);
	private static final AttributeSet STYLE_OTHERS = normal(Color.GRAY);

	private final TextFilterField textFilter =
		new TextFilterField(" Text Search (Alt-F)");
	private final ItemTextPane textArea;

	private final LogFormatter logFormatter = new DefaultLogFormatter();
	private final Map<String, AttributeSet> streamStyles =
		new ConcurrentHashMap<>();

	private LogRecorder recorder;

	// -- constructor --

	public LoggingPanel(Context context) {
		this(context.getService(ThreadService.class));
	}

	public LoggingPanel(ThreadService threadService) {
		textArea = new ItemTextPane(threadService);
		initGui();
		setPrintStreamStyle(LogRecorder.STDOUT_TAG, normal(Color.black));
		setPrintStreamStyle(LogRecorder.STDERR_TAG, normal(Color.red));
		setPrintStreamStyle(LogRecorder.GLOBAL_STDOUT_TAG, italic(Color.black));
		setPrintStreamStyle(LogRecorder.GLOBAL_STDERR_TAG, italic(Color.red));
		setRecorder(new LogRecorder());
	}

	// --- LoggingPanel methods --

	public void setRecorder(LogRecorder recorder) {
		if (recorder != null) recorder.removeObserver(textArea::update);
		this.recorder = recorder;
		updateFilter();
		recorder.addObservers(textArea::update);
	}

	public void clear() {
		recorder.clear();
		updateFilter();
	}

	public PrintStream printStream(String tag) {
		return recorder.printStream(tag);
	}

	public void setPrintStreamStyle(String tag, AttributeSet style) {
		streamStyles.put(tag, style);
	}

	// -- LogListener methods --

	@Override
	public void messageLogged(LogMessage message) {
		recorder.messageLogged(message);
	}

	// -- OutputListener methods --

	@Override
	public void outputOccurred(OutputEvent event) {
		recorder.outputOccurred(event);
	}

	// -- Helper methods --

	private void initGui() {
		textFilter.setChangeListener(this::updateFilter);

		textArea.getJComponent().setPreferredSize(new Dimension(200, 100));

		this.setLayout(new MigLayout("insets 0", "[grow]", "[][grow]"));
		this.add(textFilter.getComponent(), "grow, wrap");
		this.add(textArea.getJComponent(), "grow");
	}

	private void updateFilter() {
		final Predicate<String> quickSearchFilter = textFilter.getFilter();
		Stream<ItemTextPane.Item> stream = recorder.stream().map(this::wrapToItem)
			.filter(item -> quickSearchFilter.test(item.text()));
		textArea.setData(stream.iterator());
	}

	private ItemTextPane.Item wrapToItem(Object o) {
		if (o instanceof LogMessage)
			return wrapLogMessage((LogMessage) o);
		if (o instanceof LogRecorder.TaggedLine)
			return wrapStreamItem((LogRecorder.TaggedLine) o);
		throw new IllegalStateException();
	}

	private ItemTextPane.Item wrapLogMessage(LogMessage message) {
		return new ItemTextPane.Item(getLevelStyle(message.level()),
			logFormatter.format(message), null, true);
	}

	private ItemTextPane.Item wrapStreamItem(LogRecorder.TaggedLine item) {
		return new ItemTextPane.Item(streamStyles.getOrDefault(item.tag(),
			DEFAULT_STYLE), appendLn(item.line()), item.tag(), item.isComplete());
	}

	private static String appendLn(String text) {
		return text.endsWith("\n") ? text : text + "\n";
	}

	private static AttributeSet getLevelStyle(int i) {
		switch (i) {
			case LogLevel.ERROR:
				return STYLE_ERROR;
			case LogLevel.WARN:
				return STYLE_WARN;
			case LogLevel.INFO:
				return STYLE_INFO;
			case LogLevel.DEBUG:
				return STYLE_DEBUG;
			case LogLevel.TRACE:
				return STYLE_TRACE;
			default:
				return STYLE_OTHERS;
		}
	}

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

	// -- Helper methods - testing --

	JTextPane getTextPane() {
		return textArea.getTextPane();
	}
}
