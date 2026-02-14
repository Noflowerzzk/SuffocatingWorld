package noflower.suffocatingworld.mixin.client;

import noflower.suffocatingworld.client.SuffocatingWorldPresetEditor;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(WorldCreationUiState.class)
public abstract class WorldCreationUiStateMixin {
	private static final Identifier PRESET_ID = Identifier.tryParse("suffocating-world:suffocating_world");
	private static final ResourceKey<WorldPreset> SUFFOCATING_PRESET =
		PRESET_ID == null ? null : ResourceKey.create(net.minecraft.core.registries.Registries.WORLD_PRESET, PRESET_ID);

	@Shadow
	public abstract WorldCreationUiState.WorldTypeEntry getWorldType();

	@Inject(method = "getPresetEditor", at = @At("HEAD"), cancellable = true)
	private void suffocatingworld$overridePresetEditor(CallbackInfoReturnable<PresetEditor> cir) {
		WorldCreationUiState.WorldTypeEntry entry = getWorldType();
		if (entry == null || entry.preset() == null) {
			return;
		}

		if (SUFFOCATING_PRESET == null) {
			return;
		}

		Optional<ResourceKey<WorldPreset>> key = entry.preset().unwrapKey();
		if (key.isPresent() && key.get().equals(SUFFOCATING_PRESET)) {
			cir.setReturnValue(new SuffocatingWorldPresetEditor());
		}
	}
}
