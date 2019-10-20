package de.leonhard.storage;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;
import de.leonhard.storage.internal.FileData;
import de.leonhard.storage.internal.FileType;
import de.leonhard.storage.internal.FlatFile;
import de.leonhard.storage.internal.IStorage;
import de.leonhard.storage.internal.editor.YamlEditor;
import de.leonhard.storage.internal.editor.YamlParser;
import de.leonhard.storage.internal.settings.ConfigSettings;
import de.leonhard.storage.internal.settings.ReloadSettings;
import de.leonhard.storage.utils.FileUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;

import java.io.*;
import java.util.*;

@Getter
@SuppressWarnings("unchecked")
public class Yaml extends FlatFile implements IStorage {
	private final YamlEditor yamlEditor;
	private final YamlParser parser;
	@Setter
	private ConfigSettings configSettings = ConfigSettings.SKIP_COMMENTS;

	public Yaml(final String name, final String path) {
		this(name, path, null, null);
	}

	public Yaml(final String name, final String path, final InputStream inputStream) {
		this(name, path, inputStream, null);
	}

	public Yaml(final String name, final String path, final InputStream inputStream, final ReloadSettings reloadSettings) {
		if (create(name, path, FileType.YAML)) {
			if (inputStream != null) {
				FileUtils.writeToFile(file, inputStream);
			}
		}

		yamlEditor = new YamlEditor(file);
		parser = new YamlParser(yamlEditor);
		update();
		if (reloadSettings != null) {
			this.reloadSettings = reloadSettings;
		}
	}

	public static boolean isYaml(final String fileName) {
		return (fileName.lastIndexOf(".") > 0 ? fileName.substring(fileName.lastIndexOf(".") + 1) : "").equals("yml");
	}

	protected final boolean isYaml(final File file) {
		return isYaml(file.getName());
	}

	@Override
	public void set(final String key, final Object value) {
		set(key, value, this.configSettings);
	}

	@Synchronized
	public void set(final String key, final Object value, final ConfigSettings configSettings) {
		reload();

		final String finalKey = (pathPrefix == null) ? key : pathPrefix + "." + key;


		String old = fileData.toString();
		fileData.insert(finalKey, value);

		if (fileData != null && old.equals(fileData.toString())) {
			return;
		}

		try {
			if (!ConfigSettings.PRESERVE_COMMENTS.equals(configSettings)) {
				write(Objects.requireNonNull(fileData).toMap());
				return;
			}
			final List<String> unEdited = yamlEditor.read();
			final List<String> header = yamlEditor.readHeader();
			final List<String> footer = yamlEditor.readFooter();
			write(fileData.toMap());
			header.addAll(yamlEditor.read());
			if (!header.containsAll(footer)) {
				header.addAll(footer);
			}
			yamlEditor.write(parser.parseComments(unEdited, header));
			write(Objects.requireNonNull(fileData).toMap());
		} catch (final IOException e) {
			System.err.println("Error while writing '" + getName() + "'");
		}
	}

	public void write(final Map data) throws IOException {
		YamlWriter writer = new YamlWriter(new FileWriter(getFile()));
		writer.write(data);
		writer.close();
	}

	@Override
	protected void update() {
		YamlReader reader = null;
		try {
			reader = new YamlReader(new FileReader(getFile()));// Needed?
			Map<String, Object> map = (Map<String, Object>) reader.read();
			if (map == null) {
				map = new HashMap<>();
			}
			fileData = new FileData(map);
		} catch (IOException e) {
			System.err.println("Exception while reloading yaml");
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				System.err.println("Exception while closing file");
				e.printStackTrace();
			}
		}
	}

	public List<String> getHeader() {
		try {
			return yamlEditor.readHeader();
		} catch (IOException e) {
			System.err.println("Error while getting header of '" + getName() + "'");
			e.printStackTrace();
		}
		return new ArrayList<>();
	}

	@Override
	public void remove(final String key) {
		final String finalKey = (pathPrefix == null) ? key : pathPrefix + "." + key;
		fileData.remove(finalKey);

		try {
			write(fileData.toMap());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		} else if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		} else {
			Yaml yaml = (Yaml) obj;
			return this.fileData.equals(yaml.fileData)
					&& this.pathPrefix.equals(yaml.pathPrefix)
					&& this.configSettings.equals(yaml.configSettings)
					&& super.equals(yaml.getFlatFileInstance());
		}
	}
}