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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.swing.*;
import javax.swing.plaf.basic.BasicArrowButton;

import net.miginfocom.swing.MigLayout;

import org.scijava.console.OutputListener;
import org.scijava.log.IgnoreAsCallingClass;
import org.scijava.log.LogListener;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.thread.ThreadService;

/**
 * {@link AbstractConsolePanel} can display log message and console output as a list,
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
public abstract class AbstractConsolePanel<T, R extends AbstractRecorder<T>> extends JPanel
{

	private final TextFilterField textFilter =
		new TextFilterField(" Text Search (Alt-F)");

	private final ItemTextPane textArea;

	private final JPanel textFilterPanel = new JPanel();

	private R recorder;

	private JPopupMenu menu = initMenu();

	private Function<? super T, ItemTextPane.Item> filter = ignore -> null;

	// -- constructor --

	public AbstractConsolePanel(ThreadService threadService, R recorder) {
		textArea = new ItemTextPane(threadService);
		initGui();
		setRecorder(recorder);
	}

	// --- LoggingPanel methods --

	protected JPanel topPanel() {
		return textFilterPanel;
	}

	protected JComponent mainArea() {
		return textArea.getJComponent();
	}

	protected JPopupMenu popupMenu() {
		return menu;
	}

	protected void setFilter(Function<? super T, ItemTextPane.Item> filter) {
		this.filter = filter;
		updateFilter();
	}

	public void setRecorder(R recorder) {
		if (recorder != null) recorder.removeObserver(textArea::update);
		this.recorder = recorder;
		updateFilter();
		recorder.addObservers(textArea::update);
	}

	public R getRecorder() {
		return recorder;
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

	// -- Helper methods --

	private void initGui() {
		textFilter.setChangeListener(this::updateFilter);

		JButton menuButton = new BasicArrowButton(BasicArrowButton.SOUTH);
		menuButton.addActionListener(a ->
			menu.show(menuButton, 0, menuButton.getHeight()));

		textFilterPanel.setLayout(new MigLayout("insets 0", "[][grow]", "[]"));
		textFilterPanel.add(menuButton);
		textFilterPanel.add(textFilter.getComponent(), "grow");


		textArea.setPopupMenu(menu);
		textArea.getJComponent().setPreferredSize(new Dimension(200, 100));

		registerKeyStroke("alt F", "focusTextFilter", this::focusTextFilter);
	}

	public void registerKeyStroke(String keyStroke, String id, final Runnable action) {
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
		return menu;
	}

	protected static JMenuItem newMenuItem(String text, String keyStroke,
		Runnable runnable)
	{
		JMenuItem item = newMenuItem(text, runnable);
		item.setAccelerator(KeyStroke.getKeyStroke(keyStroke));
		return item;
	}

	protected static JMenuItem newMenuItem(String text, Runnable runnable) {
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(actionEvent -> runnable.run());
		return item;
	}

	private void updateFilter() {
		final Predicate<String> quickSearchFilter = textFilter.getFilter();
		Stream<ItemTextPane.Item> stream = recorder.stream().map(filter)
				.filter(x -> x != null && quickSearchFilter.test(x.text()));
		textArea.setData(stream.iterator());
	}

	// -- Helper methods - testing --

	JTextPane getTextPane() {
		return textArea.getTextPane();
	}
}
