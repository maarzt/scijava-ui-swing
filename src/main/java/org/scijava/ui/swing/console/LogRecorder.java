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

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.scijava.console.OutputListener;
import org.scijava.log.CallingClassUtils;
import org.scijava.log.IgnoreAsCallingClass;
import org.scijava.log.LogListener;
import org.scijava.log.LogMessage;
import org.scijava.log.LogService;

/**
 * {@link org.scijava.log.LogRecorder} can be used to decouple a GUI displaying log messages and
 * console output from the potentially highly concurrent code producing this log
 * messages and text.
 * <p>
 * LogRecorder can record {@link LogMessage}s and text outputted to
 * {@link PrintStream}s at the same time. It implements the {@link LogListener}
 * and {@link OutputListener} interfaces, used by {@link LogService} and
 * {@link org.scijava.console.ConsoleService}
 * </p>
 * <p>
 * The recorded {@link LogMessage}s are stored in a list. Items in the
 * list are either instances of {@link LogMessage} or {@link TaggedLine}. New
 * items are always added to the end of the list. The items are therefor sorted
 * chronologically.
 * </p>
 *
 * TODO: Update text
 * 
 * @author Matthias Arzt
 */
@IgnoreAsCallingClass
public class LogRecorder extends AbstractRecorder<LogMessage> implements LogListener, Iterable<LogMessage>
{
	private boolean recordCallingClass = false;

	public boolean isRecordCallingClass() {
		return recordCallingClass;
	}

	public void setRecordCallingClass(boolean enable) {
		this.recordCallingClass = enable;
	}

	// -- LogListener methods --

	@Override
	public void messageLogged(LogMessage message) {
		if (recordCallingClass) message.attach(CallingClassUtils.getCallingClass());
		add(message);
		notifyListeners();
	}
}
