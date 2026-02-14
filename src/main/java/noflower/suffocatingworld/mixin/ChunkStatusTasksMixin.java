package noflower.suffocatingworld.mixin;

import noflower.suffocatingworld.SuffocatingWorldConfig;
import noflower.suffocatingworld.worldgen.AirReplacement;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(net.minecraft.world.level.chunk.status.ChunkStatusTasks.class)
public class ChunkStatusTasksMixin {
	@Inject(method = "generateFeatures", at = @At("TAIL"))
	private static void suffocatingworld$replaceAirAfterFeatures(
		WorldGenContext context,
		ChunkStep step,
		StaticCache2D<GenerationChunkHolder> cache,
		ChunkAccess chunk,
		CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
	) {
		if (!SuffocatingWorldConfig.isEnabledForLevel(context.level())) {
			return;
		}

		AirReplacement.replaceAirInChunk(chunk, SuffocatingWorldConfig.replacementState());
	}
}
