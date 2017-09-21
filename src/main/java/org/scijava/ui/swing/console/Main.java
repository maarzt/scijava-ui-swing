package org.scijava.ui.swing.console;

import net.miginfocom.swing.MigLayout;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.console.ConsoleService;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;

import javax.swing.*;
import java.awt.*;

/**
 * Created by arzt on 21.06.17.
 */
public class Main {

	public static void main2(String... args) throws InterruptedException {
		Context context = new Context();
		ModuleService moduleService = context.service(ModuleService.class);
		moduleService.addModule(new CommandInfo(PluginThatLogs.class));
		UIService ui = context.service(UIService.class);
		ui.showUI();
	}

	public static void main3(String... args) throws InterruptedException {
		Context context = new Context(ThreadService.class);
		LoggingPanel loggingPanel = new LoggingPanel(context);
		JFrame frame = showFrame(loggingPanel);
		LogService logService = context.service(LogService.class);
		logService.addLogListener(loggingPanel);
		while (frame.isVisible()) {
			logService.warn("Message Text");
			System.out.println("Hello World");
			Thread.sleep(500);
		}
	}

	private static JFrame showFrame(JComponent component) {
		JFrame frame = new JFrame("test");
		frame.setSize(500, 500);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.add(component);
		frame.pack();
		frame.setVisible(true);
		return frame;
	}

	public static void main(String... args) throws InterruptedException {
		Context context = new Context(ThreadService.class, ConsoleService.class);
		ConsolePanel panel = new ConsolePanel(context);
		JFrame frame = showFrame(panel);
		ConsoleService consoleService = context.service(ConsoleService.class);
		consoleService.addOutputListener(panel);
		while (frame.isVisible()) {
			System.out.println("Hello World");
			Thread.sleep(500);
		}
	}

	@Plugin(type = Command.class, menuPath = "Log>Plugin that logs")
	public static class PluginThatLogs implements Command {

		@Parameter
		Context context;

		@Parameter
		Logger logger;

		@Override
		public void run() {
			LoggingPanel loggingPanel = new LoggingPanel(context);
			logger.addLogListener(loggingPanel);

			JFrame frame = new JFrame("Plugin that logs");
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.setLayout(new MigLayout("","[grow]","[][grow]"));
			frame.add(newButton("log", () -> logger.warn("Hello World!")), "wrap");
			frame.add(loggingPanel, "grow");
			frame.pack();
			frame.setVisible(true);
		}

		private Component newButton(String title, Runnable action) {
			JButton button = new JButton(title);
			button.addActionListener(a -> action.run());
			return button;
		}
	}
}
