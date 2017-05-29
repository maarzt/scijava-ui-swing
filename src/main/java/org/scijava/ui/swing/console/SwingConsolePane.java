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

import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JTextPane;

import net.miginfocom.swing.MigLayout;

import org.scijava.Context;
import org.scijava.console.OutputEvent;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.thread.ThreadService;
import org.scijava.ui.console.AbstractConsolePane;
import org.scijava.ui.console.ConsolePane;

/**
 * Swing implementation of {@link ConsolePane}.
 *
 * @author Curtis Rueden
 */
public class SwingConsolePane extends AbstractConsolePane<JPanel> {

	@Parameter
	private ThreadService threadService;

	@Parameter
	private LogService logService;

	private LoggingPanel loggingPanel;

	/**
	 * The console pane's containing window; e.g., a {@link javax.swing.JFrame} or
	 * {@link javax.swing.JInternalFrame}.
	 */
	private Component window;

	private JPanel component;

	public SwingConsolePane(final Context context) {
		super(context);
	}

	// -- SwingConsolePane methods --

	/** Sets the window which should be shown when {@link #show()} is called. */
	public void setWindow(final Component window) {
		this.window = window;
	}

	public void clear() {
		loggingPanel().clear();
	}

	// -- ConsolePane methods --

	@Override
	public void append(final OutputEvent event) {
		loggingPanel().outputOccurred(event);
	}

	@Override
	public void show() {
		if (window == null || window.isVisible()) return;
		threadService.queue(new Runnable() {

			@Override
			public void run() {
				window.setVisible(true);
			}
		});
	}

	// -- UIComponent methods --

	@Override
	public JPanel getComponent() {
		if (loggingPanel == null) initLoggingPanel();
		return component;
	}

	@Override
	public Class<JPanel> getComponentType() {
		return JPanel.class;
	}

	// -- Helper methods - lazy initialization --

	private LoggingPanel loggingPanel() {
		if (loggingPanel == null) initLoggingPanel();
		return loggingPanel;
	}

	private synchronized void initLoggingPanel() {
		if (loggingPanel != null) return;
		loggingPanel = new LoggingPanel(threadService);
		logService.addListener(loggingPanel);
		component = new JPanel(new MigLayout("", "[grow]", "[grow]"));
		component.add(loggingPanel, "grow");
	}

	// -- Helper methods - testing --

	JTextPane getTextPane() {
		return loggingPanel().getTextPane();
	}
}
