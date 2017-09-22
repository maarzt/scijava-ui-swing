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
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.swing.*;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import net.miginfocom.swing.MigLayout;

import org.scijava.Context;
import org.scijava.console.OutputListener;
import org.scijava.log.IgnoreAsCallingClass;
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
 * {@link Logger} and
 * {@link org.scijava.console.ConsoleService}
 *
 * @see LogService
 * @see Logger
 * @see org.scijava.console.ConsoleService
 * @author Matthias Arzt
 */
@IgnoreAsCallingClass
public class LoggingPanel extends JPanel implements LogListener
{

	private static final AttributeSet STYLE_ERROR = normal(new Color(200, 0, 0));
	private static final AttributeSet STYLE_WARN = normal(new Color(200, 140, 0));
	private static final AttributeSet STYLE_INFO = normal(Color.BLACK);
	private static final AttributeSet STYLE_DEBUG = normal(new Color(0, 0, 200));
	private static final AttributeSet STYLE_TRACE = normal(Color.GRAY);
	private static final AttributeSet STYLE_OTHERS = normal(Color.GRAY);

	private final TextFilterField textFilter =
		new TextFilterField(" Text Search (Alt-F)");
	private final LogSourcesPanel sourcesPanel = initSourcesPanel();

	private final ItemTextPane textArea;

	private final JPanel textFilterPanel = new JPanel();
	private final JSplitPane splitPane =
		new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

	private final Set<LogSource> sources = Collections.newSetFromMap(
		new ConcurrentHashMap<>());
	private final DefaultLogFormatter logFormatter = new DefaultLogFormatter();

	private LogRecorder recorder;

	// -- constructor --

	public LoggingPanel(Context context) {
		this(context.getService(ThreadService.class));
	}

	public LoggingPanel(ThreadService threadService) {
		textArea = new ItemTextPane(threadService);
		initGui();
		setRecorder(new LogRecorder());
	}

	// --- LoggingPanel methods --

	public void setRecorder(LogRecorder recorder) {
		if (recorder != null) recorder.removeObserver(textArea::update);
		this.recorder = recorder;
		updateFilter();
		recorder.addObservers(textArea::update);
	}

	public void toggleSourcesPanel() {
		boolean visible = !sourcesPanel.isVisible();
		setSourcesPanelVisible(visible);
	}

	public void setSourcesPanelVisible(boolean visible) {
		sourcesPanel.setVisible(visible);
		if (visible) reloadSources();
		splitPane.resetToPreferredSizes();
		splitPane.setDividerSize(visible ? 5 : 0);
	}

	public void setTextFilterVisible(boolean visible) {
		textFilterPanel.setVisible(visible);
	}

	public void copySelectionToClipboard() {
		textArea.copySelectionToClipboard();
	}

	public void focusTextFilter() {
		textFilter.getComponent().requestFocus();
	}

	public void clear() {
		recorder.clear();
		updateFilter();
	}

	// -- LogListener methods --

	@Override
	public void messageLogged(LogMessage message) {
		sources.add(message.source());
		recorder.messageLogged(message);
	}

	// -- Helper methods --

	private void initGui() {
		textFilter.setChangeListener(this::updateFilter);

		JPopupMenu menu = initMenu();

		JButton menuButton = new BasicArrowButton(BasicArrowButton.SOUTH);
		menuButton.addActionListener(a ->
			menu.show(menuButton, 0, menuButton.getHeight()));

		textFilterPanel.setLayout(new MigLayout("insets 0", "[][grow]", "[]"));
		textFilterPanel.add(menuButton);
		textFilterPanel.add(textFilter.getComponent(), "grow");

		sourcesPanel.setChangeListener(this::updateFilter);
		sourcesPanel.setVisible(false);

		textArea.setPopupMenu(menu);
		textArea.getJComponent().setPreferredSize(new Dimension(200, 100));

		splitPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		splitPane.setResizeWeight(0.15);
		splitPane.add(sourcesPanel);
		splitPane.add(textArea.getJComponent());
		splitPane.setDividerSize(0);
		splitPane.resetToPreferredSizes();

		this.setLayout(new MigLayout("insets 0", "[grow]", "[][grow]"));
		this.add(textFilterPanel, "grow, wrap");
		this.add(splitPane, "grow");

		registerKeyStroke("alt F", "focusTextFilter", this::focusTextFilter);
	}

	private LogSourcesPanel initSourcesPanel() {
		JButton reloadButton = new JButton("reload");
		reloadButton.addActionListener(actionEvent -> reloadSources());
		return new LogSourcesPanel(reloadButton);
	}

	private void reloadSources() {
		sourcesPanel.updateSources(sources);
	}

	private void registerKeyStroke(String keyStroke, String id, final Runnable action) {
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke
			.getKeyStroke(keyStroke), id);
		getActionMap().put(id, new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				action.run();
			}
		});
	}

	private JPopupMenu initMenu() {
		JPopupMenu menu = new JPopupMenu();
		menu.add(newMenuItem("Copy", "control C",
			this::copySelectionToClipboard));
		registerKeyStroke("control C", "copyToClipBoard",
			this::copySelectionToClipboard);
		menu.add(newMenuItem("Clear", "alt C",
			this::clear));
		registerKeyStroke("alt C", "clearLoggingPanel",
			this::clear);
		menu.add(newMenuItem("Log Sources",
			this::toggleSourcesPanel));
		menu.add(newMenuItem("update", this::updateFilter));
		menu.add(initSettingsMenu());
		return menu;
	}

	private JMenu initSettingsMenu() {
		JMenu menu = new JMenu("Settings");
		menu.add(newMenuItem("switch recording of calling class on/of",
			this::recordCallingClassSetter));
		menu.add(newMenuItem("show / hide time stamp",
			toggleLogFormatterFieldVisible(DefaultLogFormatter.Field.TIME)));
		menu.add(newMenuItem("show / hide log source",
			toggleLogFormatterFieldVisible(DefaultLogFormatter.Field.SOURCE)));
		menu.add(newMenuItem("show / hide log level",
			toggleLogFormatterFieldVisible(DefaultLogFormatter.Field.LEVEL)));
		menu.add(newMenuItem("show / hide exception",
			toggleLogFormatterFieldVisible(DefaultLogFormatter.Field.THROWABLE)));
		menu.add(newMenuItem("show / hide attached data",
			toggleLogFormatterFieldVisible(DefaultLogFormatter.Field.ATTACHMENT)));
		return menu;
	}

	private void recordCallingClassSetter() {
		if (recorder != null) {
			boolean enable = !recorder.isRecordCallingClass();
			recorder.setRecordCallingClass(enable);
			if (enable)
				logFormatter.setVisible(DefaultLogFormatter.Field.ATTACHMENT, true);
		}
	}

	private Runnable toggleLogFormatterFieldVisible(DefaultLogFormatter.Field item) {
		return () -> {
			logFormatter.setVisible(item, !logFormatter.isVisible(item));
			updateFilter();
		};
	}

	static private JMenuItem newMenuItem(String text, String keyStroke,
		Runnable runnable)
	{
		JMenuItem item = newMenuItem(text, runnable);
		item.setAccelerator(KeyStroke.getKeyStroke(keyStroke));
		return item;
	}

	static private JMenuItem newMenuItem(String text, Runnable runnable) {
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(actionEvent -> runnable.run());
		return item;
	}

	private void updateFilter() {
		final Predicate<String> quickSearchFilter = textFilter.getFilter();
		final Predicate<LogMessage> logLevelFilter = sourcesPanel.getFilter();
		DefaultLogFormatter logFormatter = this.logFormatter;
		Function<LogMessage, ItemTextPane.Item> filter = logMessage -> {
			if (!logLevelFilter.test(logMessage)) return null;
			ItemTextPane.Item item = new ItemTextPane.Item(getLevelStyle(logMessage.level()),
					logFormatter.format(logMessage), null, true);
			if (!quickSearchFilter.test(item.text())) return null;
			return item;
		};
		Stream<ItemTextPane.Item> stream = recorder.stream().map(filter).filter(Objects::nonNull);
		textArea.setData(stream.iterator());
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

	// -- Helper methods - testing --

	JTextPane getTextPane() {
		return textArea.getTextPane();
	}
}
