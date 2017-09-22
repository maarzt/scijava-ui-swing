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
import java.io.PrintStream;
import java.util.Collections;
import java.util.Map;
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
import org.scijava.console.OutputEvent;
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
 * {@link ConsolePanel} can display log message and console output as a list,
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
public class ConsolePanel extends JPanel implements OutputListener
{

	private final TextFilterField textFilter =
		new TextFilterField(" Text Search (Alt-F)");
	private final ItemTextPane textArea;

	private final JPanel textFilterPanel = new JPanel();

	private final Map<String, AttributeSet> streamStyles =
		new ConcurrentHashMap<>();

	private ConsoleRecorder recorder = new ConsoleRecorder();

	// -- constructor --

	public ConsolePanel(Context context) {
		this(context.getService(ThreadService.class));
	}

	public ConsolePanel(ThreadService threadService) {
		textArea = new ItemTextPane(threadService);
		initGui();
		recorder.addObservers(textArea::update);
		updateFilter();
	}

	// --- LoggingPanel methods --


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

	public PrintStream printStream(AttributeSet style) {
		return recorder.printStream(new SimpleAttributeSet(style));
	}

	// -- OutputListener methods --

	@Override
	public void outputOccurred(OutputEvent event) {
		recorder.outputOccurred(event);
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

		textArea.setPopupMenu(menu);
		textArea.getJComponent().setPreferredSize(new Dimension(200, 100));

		this.setLayout(new MigLayout("insets 0", "[grow]", "[][grow]"));
		this.add(textFilterPanel, "grow, wrap");
		this.add(textArea.getJComponent(), "grow");

		registerKeyStroke("alt F", "focusTextFilter", this::focusTextFilter);
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
		registerKeyStroke("alt C", "clearConsolePanel",
			this::clear);
		menu.add(newMenuItem("update", this::updateFilter));
		return menu;
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
		Stream<ItemTextPane.Item> stream =
			recorder.stream().filter(item -> quickSearchFilter.test(item.text()));
		textArea.setData(stream.iterator());
	}

	// -- Helper methods - testing --

	JTextPane getTextPane() {
		return textArea.getTextPane();
	}
}
