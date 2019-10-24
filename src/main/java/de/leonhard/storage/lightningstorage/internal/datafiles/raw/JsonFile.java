package de.leonhard.storage.lightningstorage.internal.datafiles.raw;

import de.leonhard.storage.lightningstorage.internal.base.FileData;
import de.leonhard.storage.lightningstorage.internal.base.FlatFile;
import de.leonhard.storage.lightningstorage.internal.base.enums.ReloadSetting;
import de.leonhard.storage.lightningstorage.utils.FileUtils;
import de.leonhard.storage.lightningstorage.utils.JsonUtils;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Cleanup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;


@SuppressWarnings({"unchecked", "unused"})
public class JsonFile extends FlatFile {

	public JsonFile(@NotNull final File file, @Nullable final InputStream inputStream, @Nullable final ReloadSetting reloadSetting, @Nullable final FileData.Type fileDataType) {
		super(file, FileType.JSON);
		if (create() && inputStream != null) {
			FileUtils.writeToFile(this.file, inputStream);
		}

		if (fileDataType != null) {
			setFileDataType(fileDataType);
		} else {
			setFileDataType(FileData.Type.STANDARD);
		}

		reload();
		if (reloadSetting != null) {
			setReloadSetting(reloadSetting);
		}
	}


	@Override
	public void reload() {
		final JSONTokener jsonTokener = new JSONTokener(Objects.requireNonNull(FileUtils.createNewInputStream(file)));
		fileData = new FileData(new JSONObject(jsonTokener));
	}

	/**
	 * Gets a Map by key Although used to get nested objects {@link JsonFile}
	 *
	 * @param key Path to Map-List in JSON
	 * @return Map
	 */

	@Override
	public Map getMap(@NotNull final String key) {
		String tempKey = (this.getPathPrefix() == null) ? key : this.getPathPrefix() + "." + key;
		if (!hasKey(tempKey)) {
			return new HashMap();
		} else {
			return getMapWithoutPath(tempKey);
		}
	}

	private Map getMapWithoutPath(@NotNull final String key) {
		update();

		if (!hasKey(key)) {
			return new HashMap<>();
		}

		Object map;
		try {
			map = getObject(key);
		} catch (JSONException e) {
			return new HashMap<>();
		}
		if (map instanceof Map) {
			return (Map<?, ?>) fileData.get(key);
		} else if (map instanceof JSONObject) {
			return JsonUtils.jsonToMap((JSONObject) map);
		}
		throw new IllegalArgumentException("Json does not contain: '" + key + "'.");
	}

	private Object getObject(@NotNull final String key) {
		if (!hasKey(key)) {
			return null;
		}

		if (key.contains(".")) {
			return new FileData(fileData.toMap()).containsKey(key) ? new FileData(fileData.toMap()).get(key) : null;
		}
		return fileData.containsKey(key) ? fileData.get(key) : null;
	}

	@Override
	public <T> T getOrSetDefault(@NotNull final String key, @NotNull T value) {
		if (!hasKey(key)) {
			set(key, value);
			return value;
		} else {
			return (T) get(key);
		}
	}

	@SuppressWarnings("Duplicates")
	@Override
	public synchronized void set(@NotNull final String key, @Nullable final Object value) {
		if (insert(key, value)) {
			try {
				write(new JSONObject(fileData.toMap()));
			} catch (IOException e) {
				System.err.println("Error while writing to '" + file.getAbsolutePath() + "'");
				e.printStackTrace();
				throw new IllegalStateException();
			}
		}
	}

	private void write(final JSONObject object) throws IOException {
		@Cleanup Writer writer = new PrintWriter(new FileWriter(getFile().getAbsolutePath()));
		writer.write(object.toString(3));
	}

	@Override
	public Object get(@NotNull final String key) {

		String finalKey = (this.getPathPrefix() == null) ? key : this.getPathPrefix() + "." + key;

		return getObject(finalKey);
	}

	@Override
	public boolean getBoolean(@NotNull final String key) {
		String finalKey = (this.getPathPrefix() == null) ? key : this.getPathPrefix() + "." + key;
		return super.getBoolean(finalKey);
	}

	@Override
	public byte getByte(@NotNull final String key) {
		String finalKey = (this.getPathPrefix() == null) ? key : this.getPathPrefix() + "." + key;
		return super.getByte(finalKey);
	}

	@Override
	public List<Byte> getByteList(@NotNull final String key) {
		String finalKey = (this.getPathPrefix() == null) ? key : this.getPathPrefix() + "." + key;
		return super.getByteList(finalKey);
	}

	@Override
	public double getDouble(@NotNull final String key) {
		String finalKey = (this.getPathPrefix() == null) ? key : this.getPathPrefix() + "." + key;
		return super.getDouble(finalKey);
	}

	@Override
	public float getFloat(@NotNull final String key) {
		String finalKey = (this.getPathPrefix() == null) ? key : this.getPathPrefix() + "." + key;
		return super.getFloat(finalKey);
	}

	@Override
	public int getInt(@NotNull final String key) {
		String finalKey = (this.getPathPrefix() == null) ? key : this.getPathPrefix() + "." + key;
		return super.getInt(finalKey);
	}

	@Override
	public List<Integer> getIntegerList(@NotNull final String key) {
		String finalKey = (this.getPathPrefix() == null) ? key : this.getPathPrefix() + "." + key;
		return super.getIntegerList(finalKey);
	}

	@Override
	public List<?> getList(@NotNull final String key) {
		String finalKey = (this.getPathPrefix() == null) ? key : this.getPathPrefix() + "." + key;
		return super.getList(finalKey);
	}

	@Override
	public long getLong(@NotNull final String key) {
		String finalKey = (this.getPathPrefix() == null) ? key : this.getPathPrefix() + "." + key;
		return super.getLong(finalKey);
	}

	@Override
	public List<Long> getLongList(@NotNull final String key) {
		String finalKey = (this.getPathPrefix() == null) ? key : this.getPathPrefix() + "." + key;
		return super.getLongList(finalKey);
	}

	@Override
	public String getString(@NotNull final String key) {
		String finalKey = (this.getPathPrefix() == null) ? key : this.getPathPrefix() + "." + key;
		return super.getString(finalKey);
	}

	@Override
	public synchronized void remove(@NotNull final String key) {
		final String finalKey = (this.getPathPrefix() == null) ? key : this.getPathPrefix() + "." + key;

		update();

		if (fileData.containsKey(finalKey)) {
			fileData.remove(finalKey);
			try {
				write(fileData.toJsonObject());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Sets a value to the json if the file doesn't already contain the value
	 * (Not mix up with Bukkit addDefault) Uses {@link JSONObject}
	 *
	 * @param key   Key to set the value
	 * @param value Value to set
	 */

	@Override
	public void setDefault(@NotNull final String key, @Nullable final Object value) {
		if (hasKey(key)) {
			return;
		}
		set(key, value);
	}

	protected final JsonFile getJsonFileInstance() {
		return this;
	}

	@Override
	public boolean equals(@Nullable final Object obj) {
		if (obj == this) {
			return true;
		} else if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		} else {
			JsonFile json = (JsonFile) obj;
			return super.equals(json.getFlatFileInstance());
		}
	}
}