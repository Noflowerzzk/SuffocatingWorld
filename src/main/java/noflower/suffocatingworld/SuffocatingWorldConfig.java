package noflower.suffocatingworld;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

public final class SuffocatingWorldConfig {
	private static final String LEVEL_TYPE_KEY = "level-type";
	private static final String ENABLE_LEVEL_TYPE = "suffocating-world";
	private static final String ENABLE_LEVEL_TYPE_PRESET = "suffocating-world:suffocating_world";
	private static final String ENABLED_KEY = "suffocating-world.enabled";
	private static final String BLOCK_KEY = "suffocating-world.block";
	private static final String DEFAULT_BLOCK_ID = "minecraft:stone";
	private static final String CONFIG_FILE_NAME = "suffocating-world.properties";
	private static final Identifier DIMENSION_TYPE_OVERWORLD = Identifier.tryParse("suffocating-world:overworld");
	private static final Identifier DIMENSION_TYPE_NETHER = Identifier.tryParse("suffocating-world:the_nether");
	private static final Identifier DIMENSION_TYPE_END = Identifier.tryParse("suffocating-world:the_end");

	private static volatile Boolean enabledOverride;
	private static volatile boolean enabledFromLevelType;
	private static volatile BlockState replacementState = Blocks.STONE.defaultBlockState();

	private SuffocatingWorldConfig() {
	}

	public static void load() {
		Path gameDir = FabricLoader.getInstance().getGameDir();
		Path propertiesPath = gameDir.resolve("server.properties");
		Path configDir = FabricLoader.getInstance().getConfigDir();
		Path configPath = configDir.resolve(CONFIG_FILE_NAME);

		Properties serverProperties = new Properties();
		if (Files.exists(propertiesPath)) {
			try (InputStream input = Files.newInputStream(propertiesPath)) {
				serverProperties.load(input);
			} catch (IOException ex) {
				SuffocatingWorld.LOGGER.warn("Failed to read server.properties, using defaults.", ex);
			}
		} else {
			SuffocatingWorld.LOGGER.warn("server.properties not found at {}, using defaults.", propertiesPath);
		}

		Properties configProperties = new Properties();
		if (Files.exists(configPath)) {
			try (InputStream input = Files.newInputStream(configPath)) {
				configProperties.load(input);
			} catch (IOException ex) {
				SuffocatingWorld.LOGGER.warn("Failed to read {}, using defaults.", configPath, ex);
			}
		} else {
			writeDefaultConfig(configDir, configPath);
		}

		String levelType = serverProperties.getProperty(LEVEL_TYPE_KEY, "").trim();
		String levelTypeLower = levelType.toLowerCase(Locale.ROOT);
		enabledFromLevelType = ENABLE_LEVEL_TYPE.equals(levelTypeLower)
			|| ENABLE_LEVEL_TYPE_PRESET.equals(levelTypeLower)
			|| "suffocating_world".equals(levelTypeLower);

		enabledOverride = parseBooleanOrNull(configProperties.getProperty(ENABLED_KEY, "").trim());

		String blockIdRaw = firstNonBlank(
			configProperties.getProperty(BLOCK_KEY, ""),
			serverProperties.getProperty(BLOCK_KEY, ""),
			DEFAULT_BLOCK_ID
		);
		replacementState = resolveBlockState(blockIdRaw);

		if (enabledOverride != null) {
			SuffocatingWorld.LOGGER.info(
				"Suffocating World enabled override set to {}, replacing air with {}.",
				enabledOverride,
				replacementState.getBlock()
			);
		} else if (enabledFromLevelType) {
			SuffocatingWorld.LOGGER.info(
				"Suffocating World enabled by level-type ({}), replacing air with {}.",
				levelType.isEmpty() ? "<empty>" : levelType,
				replacementState.getBlock()
			);
		} else {
			SuffocatingWorld.LOGGER.info(
				"Suffocating World awaiting world preset or config enable, replacement block {}.",
				replacementState.getBlock()
			);
			SuffocatingWorld.LOGGER.info(
				"Level-type is {}, no air replacement will occur unless enabled by preset or config.",
				levelType.isEmpty() ? "<empty>" : levelType
			);
		}
	}

	public static boolean isEnabledForLevel(ServerLevel level) {
		if (Boolean.TRUE.equals(enabledOverride)) {
			return true;
		}
		if (Boolean.FALSE.equals(enabledOverride)) {
			return false;
		}
		if (enabledFromLevelType) {
			return true;
		}
		if (level == null) {
			return false;
		}

		return isCustomDimensionType(level);
	}

	public static BlockState replacementState() {
		return replacementState;
	}

	public static void setReplacementBlockId(String blockIdRaw) {
		if (blockIdRaw == null || blockIdRaw.isBlank()) {
			return;
		}
		replacementState = resolveBlockState(blockIdRaw.trim());
	}

	private static boolean isCustomDimensionType(ServerLevel level) {
		if (DIMENSION_TYPE_OVERWORLD == null || DIMENSION_TYPE_NETHER == null || DIMENSION_TYPE_END == null) {
			return false;
		}

		Holder<?> holder = level.dimensionTypeRegistration();
		return holder.is(DIMENSION_TYPE_OVERWORLD)
			|| holder.is(DIMENSION_TYPE_NETHER)
			|| holder.is(DIMENSION_TYPE_END);
	}

	private static String firstNonBlank(String first, String second, String fallback) {
		if (first != null && !first.isBlank()) {
			return first.trim();
		}
		if (second != null && !second.isBlank()) {
			return second.trim();
		}
		return fallback;
	}

	private static Boolean parseBooleanOrNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return Boolean.parseBoolean(value);
	}

	private static BlockState resolveBlockState(String blockIdRaw) {
		Identifier id;
		if (blockIdRaw.indexOf(Identifier.NAMESPACE_SEPARATOR) >= 0) {
			id = Identifier.tryParse(blockIdRaw);
		} else {
			id = Identifier.withDefaultNamespace(blockIdRaw);
		}

		if (id == null) {
			SuffocatingWorld.LOGGER.warn("Invalid block id '{}', using {}.", blockIdRaw, DEFAULT_BLOCK_ID);
			return Blocks.STONE.defaultBlockState();
		}

		Optional<Holder.Reference<Block>> holder = BuiltInRegistries.BLOCK.get(id);
		if (holder.isEmpty()) {
			SuffocatingWorld.LOGGER.warn("Unknown block id '{}', using {}.", blockIdRaw, DEFAULT_BLOCK_ID);
			return Blocks.STONE.defaultBlockState();
		}

		Block block = holder.get().value();
		if (block == Blocks.AIR) {
			SuffocatingWorld.LOGGER.warn("Block id '{}' resolves to air, using {}.", blockIdRaw, DEFAULT_BLOCK_ID);
			return Blocks.STONE.defaultBlockState();
		}

		return block.defaultBlockState();
	}

	private static void writeDefaultConfig(Path configDir, Path configPath) {
		try {
			Files.createDirectories(configDir);
			if (Files.exists(configPath)) {
				return;
			}

			Properties defaults = new Properties();
			defaults.setProperty(ENABLED_KEY, "");
			defaults.setProperty(BLOCK_KEY, DEFAULT_BLOCK_ID);
			try (OutputStream output = Files.newOutputStream(configPath)) {
				defaults.store(output, "Suffocating World configuration");
			}
		} catch (IOException ex) {
			SuffocatingWorld.LOGGER.warn("Failed to write default config to {}.", configPath, ex);
		}
	}
}
