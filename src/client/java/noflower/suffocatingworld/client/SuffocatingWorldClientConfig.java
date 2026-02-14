package noflower.suffocatingworld.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SuffocatingWorldClientConfig {
	public static final String CONFIG_FILE_NAME = "suffocating-world.properties";
	public static final String BLOCK_KEY = "suffocating-world.block";
	public static final String DEFAULT_BLOCK_ID = "minecraft:stone";

	private SuffocatingWorldClientConfig() {
	}

	public static String loadBlockId() {
		Properties props = loadProperties();
		String value = props.getProperty(BLOCK_KEY, DEFAULT_BLOCK_ID).trim();
		return value.isEmpty() ? DEFAULT_BLOCK_ID : value;
	}

	public static void saveBlockId(String blockId) {
		Properties props = loadProperties();
		props.setProperty(BLOCK_KEY, blockId);
		writeProperties(props);
	}

	private static Properties loadProperties() {
		Path configPath = getConfigPath();
		Properties props = new Properties();
		if (Files.exists(configPath)) {
			try (InputStream input = Files.newInputStream(configPath)) {
				props.load(input);
			} catch (IOException ex) {
				// ignore, defaults will be used
			}
		}
		return props;
	}

	private static void writeProperties(Properties props) {
		Path configPath = getConfigPath();
		try {
			Files.createDirectories(configPath.getParent());
			try (OutputStream output = Files.newOutputStream(configPath)) {
				props.store(output, "Suffocating World configuration");
			}
		} catch (IOException ex) {
			// ignore, best-effort write
		}
	}

	private static Path getConfigPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
	}
}
