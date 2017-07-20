package com.github.syuchan1005.mcplugindebugger;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.DefaultCaret;
import org.jetbrains.annotations.NotNull;

/**
 * Created by syuchan on 2017/07/04.
 */
public class DebugToolWindowFactory implements ToolWindowFactory {
	private JPanel mainPanel;
	private JTextField serverJarFile;
	private JButton serverBrowseButton;
	private JTextField pluginJarFile;
	private JButton pluginBrowseButton;
	private JTextArea outTextArea;
	private JTextField commandField;
	private JButton startButton;
	private JButton stopButton;
	private JButton sendCommandButton;
	private JButton reloadPluginButton;
	private JTextField pluginName;
	private JButton pluginNameSetButton;
	private JTextField connectPort;
	private JButton portSetButton;
	private JButton reloadButton;
	private JButton rebootButton;
	private JButton forceStopButton;
	private JScrollPane scrollPane;
	private JTextField hostField;
	private JButton hostSetButton;

	private Project project;
	private PluginDataConfig config;
	private ServerProcessManager processManager;

	private static FileChooserDescriptor SingleJarFileDescriptor = new FileChooserDescriptor(true, false, true, false, false, false)
			.withFileFilter((file) -> Comparing.equal(file.getExtension(), "jar", SystemInfo.isFileSystemCaseSensitive));

	@Override
	public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
		this.project = project;
		this.config = PluginDataConfig.getInstance(project);
		if (this.config != null) this.config.init(project);
		processManager = new ServerProcessManager(this.config, line -> {
			if (line.equals("STX")) outTextArea.setText("");
			else outTextArea.append(line + "\n");
		});
		((DefaultCaret) outTextArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		Content content = ContentFactory.SERVICE.getInstance().createContent(mainPanel, "", false);
		toolWindow.getContentManager().addContent(content);
		setValues();
		setListeners();
	}

	private void setValues() {
		serverJarFile.setText(config.getServerJarFile().getAbsolutePath());
		pluginJarFile.setText(config.getPluginJarFile().getAbsolutePath());
		pluginName.setText(config.getPluginName());
		hostField.setText(config.getHost());
		connectPort.setText(config.getPort().toString());
	}

	private void setListeners() {
		serverBrowseButton.addActionListener((e) -> {
			VirtualFile chooseFile = FileChooser.chooseFile(SingleJarFileDescriptor, this.project, this.project.getBaseDir());
			if (chooseFile != null) {
				config.setServerJarFile(new File(chooseFile.getPath()));
				this.serverJarFile.setText(config.getServerJarFile().getAbsolutePath());
			}
		});
		pluginBrowseButton.addActionListener((e) -> {
			VirtualFile chooseFile = FileChooser.chooseFile(SingleJarFileDescriptor, this.project, this.project.getBaseDir());
			if (chooseFile != null) {
				config.setPluginJarFile(new File(chooseFile.getPath()));
				this.pluginJarFile.setText(config.getPluginJarFile().getAbsolutePath());
			}
		});

		pluginNameSetButton.addActionListener((e) -> {
			config.setPluginName(pluginName.getText());
		});
		hostSetButton.addActionListener((e) -> {
			config.setHost(hostField.getText());
		});
		portSetButton.addActionListener((e) -> {
			config.setPort(Integer.valueOf(connectPort.getText()));
		});

		reloadPluginButton.addActionListener((e) -> {
			new Thread(() -> {
				reloadPluginButton.setEnabled(false);
				try (Socket socket = new Socket(config.getHost(), config.getPort());
					 DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
					dataOutputStream.writeUTF(config.getPluginName());
					if (!socket.isClosed()) {
						FileInputStream fileInputStream = new FileInputStream(config.getPluginJarFile());
						byte[] buffer = new byte[512];
						int fLength;
						while ((fLength = fileInputStream.read(buffer)) > 0) {
							dataOutputStream.write(buffer, 0, fLength);
						}
						fileInputStream.close();
					}
					dataOutputStream.flush();
				} catch (IOException e1) {
					e1.printStackTrace();
				} finally {
					reloadPluginButton.setEnabled(true);
				}
			}).start();
		});

		// ServerButtons
		startButton.addActionListener((e) -> {
			try {
				processManager.startServer();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});
		stopButton.addActionListener((e) -> {
			try {
				processManager.stopServer();
			} catch (InterruptedException | IOException e1) {
				e1.printStackTrace();
			}
		});
		reloadButton.addActionListener((e) -> {
			try {
				processManager.reload();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});
		rebootButton.addActionListener((e) -> {
			try {
				processManager.reboot();
			} catch (InterruptedException | IOException e1) {
				e1.printStackTrace();
			}
		});
		forceStopButton.addActionListener((e) -> {
			try {
				processManager.forceStop();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});
		commandField.addActionListener((e) -> {
			sendCommandButton.doClick();
		});
		sendCommandButton.addActionListener((e) -> {
			try {
				processManager.writeCommand(commandField.getText() + "\n");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});
	}

}
