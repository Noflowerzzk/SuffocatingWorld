package noflower.suffocatingworld.worldgen;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class AirReplacement {
	private AirReplacement() {
	}

	public static void replaceAirInChunk(ChunkAccess chunk, BlockState replacement) {
		if (replacement == null || replacement.isAir()) {
			return;
		}

		Block replacementBlock = replacement.getBlock();
		BlockState fallbackReplacement = resolveReplacementFallback(replacementBlock);
		boolean hasFallback = fallbackReplacement != null;

		LevelChunkSection[] sections = chunk.getSections();
		int minY = chunk.getMinY();
		for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
			LevelChunkSection section = sections[sectionIndex];
			if (section == null) {
				continue;
			}

			int sectionBaseY = minY + (sectionIndex * 16);
			section.acquire();
			try {
				boolean sectionAllAir = section.hasOnlyAir();
				for (int y = 0; y < 16; y++) {
					int worldY = sectionBaseY + y;
					for (int z = 0; z < 16; z++) {
						for (int x = 0; x < 16; x++) {
							if (sectionAllAir || section.getBlockState(x, y, z).isAir()) {
								BlockState target = replacement;
								if (hasFallback) {
									int belowY = worldY - 1;
									if (belowY < minY) {
										target = fallbackReplacement;
									} else {
										BlockPos belowPos = chunk.getPos().getBlockAt(x, belowY, z);
										BlockState belowState = chunk.getBlockState(belowPos);
										if (!belowState.getFluidState().isEmpty()) {
											target = fallbackReplacement;
										}
									}
								}
								section.setBlockState(x, y, z, target, false);
							}
						}
					}
				}
				section.recalcBlockCounts();
			} finally {
				section.release();
			}
		}

		chunk.markUnsaved();
	}

	private static BlockState resolveReplacementFallback(Block block) {
		if (block == Blocks.DRAGON_EGG
			|| block == Blocks.ANVIL
			|| block == Blocks.CHIPPED_ANVIL
			|| block == Blocks.DAMAGED_ANVIL
			|| block == Blocks.SUSPICIOUS_SAND
			|| block == Blocks.SUSPICIOUS_GRAVEL) {
			return Blocks.STONE.defaultBlockState();
		}

		if (block == Blocks.SAND) {
			return Blocks.SANDSTONE.defaultBlockState();
		}
		if (block == Blocks.RED_SAND) {
			return Blocks.RED_SANDSTONE.defaultBlockState();
		}
		if (block == Blocks.GRAVEL) {
			return Blocks.STONE.defaultBlockState();
		}

		if (block == Blocks.WHITE_CONCRETE_POWDER) {
			return Blocks.WHITE_CONCRETE.defaultBlockState();
		}
		if (block == Blocks.ORANGE_CONCRETE_POWDER) {
			return Blocks.ORANGE_CONCRETE.defaultBlockState();
		}
		if (block == Blocks.MAGENTA_CONCRETE_POWDER) {
			return Blocks.MAGENTA_CONCRETE.defaultBlockState();
		}
		if (block == Blocks.LIGHT_BLUE_CONCRETE_POWDER) {
			return Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState();
		}
		if (block == Blocks.YELLOW_CONCRETE_POWDER) {
			return Blocks.YELLOW_CONCRETE.defaultBlockState();
		}
		if (block == Blocks.LIME_CONCRETE_POWDER) {
			return Blocks.LIME_CONCRETE.defaultBlockState();
		}
		if (block == Blocks.PINK_CONCRETE_POWDER) {
			return Blocks.PINK_CONCRETE.defaultBlockState();
		}
		if (block == Blocks.GRAY_CONCRETE_POWDER) {
			return Blocks.GRAY_CONCRETE.defaultBlockState();
		}
		if (block == Blocks.LIGHT_GRAY_CONCRETE_POWDER) {
			return Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
		}
		if (block == Blocks.CYAN_CONCRETE_POWDER) {
			return Blocks.CYAN_CONCRETE.defaultBlockState();
		}
		if (block == Blocks.PURPLE_CONCRETE_POWDER) {
			return Blocks.PURPLE_CONCRETE.defaultBlockState();
		}
		if (block == Blocks.BLUE_CONCRETE_POWDER) {
			return Blocks.BLUE_CONCRETE.defaultBlockState();
		}
		if (block == Blocks.BROWN_CONCRETE_POWDER) {
			return Blocks.BROWN_CONCRETE.defaultBlockState();
		}
		if (block == Blocks.GREEN_CONCRETE_POWDER) {
			return Blocks.GREEN_CONCRETE.defaultBlockState();
		}
		if (block == Blocks.RED_CONCRETE_POWDER) {
			return Blocks.RED_CONCRETE.defaultBlockState();
		}
		if (block == Blocks.BLACK_CONCRETE_POWDER) {
			return Blocks.BLACK_CONCRETE.defaultBlockState();
		}

		return null;
	}
}
