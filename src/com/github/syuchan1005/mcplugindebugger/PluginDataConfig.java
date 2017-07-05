package com.github.syuchan1005.mcplugindebugger;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Property;
import java.io.File;
import org.jetbrains.annotations.Nullable;

/**
 * Created by syuchan on 2017/07/04.
 */
@State(
		name = "MCPluginDebuggerConfig",
		storages = {
				@Storage("MCPluginDebugger.xml")
		}
)
public class PluginDataConfig implements PersistentStateComponent<PluginDataConfig> {
	@Nullable
	@Override
	public PluginDataConfig getState() {
		return this;
	}

	@Override
	public void loadState(PluginDataConfig pluginDataConfig) {
		XmlSerializerUtil.copyBean(pluginDataConfig, this);
	}

	@Nullable
	public static PluginDataConfig getInstance(Project project) {
		return ServiceManager.getService(project, PluginDataConfig.class);
	}

	@Property
	private String serverJarFile;
	@Property
	private String pluginJarFile;
	@Property
	private String pluginName;
	@Property
	private String host;
	@Property
	private Integer port;

	public void init(Project project) {
		if (serverJarFile == null) setServerJarFile(new File(project.getBaseDir().getPath()));
		if (pluginJarFile == null) setPluginJarFile(new File(project.getBaseDir().getPath()));
		if (pluginName == null) pluginName = "EX";
		if (host == null) host = "localhost";
		if (port == null) port = 9000;
	}

	public File getServerJarFile() {
		return new File(serverJarFile);
	}

	public void setServerJarFile(File serverJarFile) {
		this.serverJarFile = trimPath(serverJarFile.getAbsolutePath());
	}

	public File getPluginJarFile() {
		return new File(pluginJarFile);
	}

	public void setPluginJarFile(File pluginJarFile) {
		this.pluginJarFile = trimPath(pluginJarFile.getAbsolutePath());
	}

	public String getPluginName() {
		return pluginName;
	}

	public void setPluginName(String pluginName) {
		this.pluginName = pluginName;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	private static String trimPath(String s) {
		if (s.endsWith("!")) {
			return s.substring(0, s.length() - 1);
		}
		return s;
	}
}
