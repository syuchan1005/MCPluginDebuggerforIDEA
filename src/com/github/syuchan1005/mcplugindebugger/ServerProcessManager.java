package com.github.syuchan1005.mcplugindebugger;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Created by syuchan on 2017/07/04.
 */
public class ServerProcessManager {
	private PluginDataConfig config;
	private Consumer<String> consumer;
	private Thread watchConsole;
	private Process process;

	public ServerProcessManager(PluginDataConfig config, Consumer<String> consumer) {
		this.config = config;
		this.consumer = consumer;
	}
	
	private boolean isRunning() {
		return process != null && process.isAlive();
	}

	public void startServer() throws IOException {
		if (!isRunning()) {
			ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", config.getServerJarFile().getName());
			processBuilder.directory(config.getServerJarFile().getParentFile());
			processBuilder.redirectErrorStream(true);
			process = processBuilder.start();
			if (watchConsole != null) watchConsole.interrupt();
			watchConsole = new Thread(() -> {
				this.consumer.accept("STX");
				try (InputStreamReader reader = new InputStreamReader(process.getInputStream());
					 BufferedReader bufferedReader = new BufferedReader(reader)) {
					while (isRunning() && !Thread.interrupted()) {
						try {
							String line = bufferedReader.readLine();
							if (line == null) continue;
							this.consumer.accept(line);
						} catch (Exception ignored) {}
					}
				} catch (Exception ignored) {}
			});
			watchConsole.start();
		}
	}

	public void stopServer() throws InterruptedException, IOException {
		writeCommand("stop\n");
	}

	public void reload() throws IOException {
		writeCommand("reload\n");
	}

	public void reboot() throws InterruptedException, IOException {
		stopServer();
		startServer();
	}

	public void forceStop() throws IOException {
		if (isRunning()) {
			process.getErrorStream().close();
			process.getInputStream().close();
			process.getOutputStream().close();
			process.destroy();
		}
	}

	public void writeCommand(String cmd) throws IOException {
		if (isRunning()) {
			OutputStream outputStream = process.getOutputStream();
			outputStream.write(cmd.getBytes(StandardCharsets.UTF_8));
			outputStream.flush();
			consumer.accept("> " + cmd.substring(0, cmd.length() - 1));
		}
	}
}
